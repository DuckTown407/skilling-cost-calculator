package com.skillingcost;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One pure calculation path for both comparison and detail views. */
final class CostCalculator
{
    static final class TaxSettings
    {
        final int basisPoints;
        final long capGp;
        final long exemptBelowGp;

        TaxSettings(int basisPoints, long capGp, long exemptBelowGp)
        {
            this.basisPoints = basisPoints;
            this.capGp = capGp;
            this.exemptBelowGp = exemptBelowGp;
        }

        boolean valid()
        {
            return basisPoints >= 0 && basisPoints <= 10_000 && capGp >= 0 && exemptBelowGp >= 0;
        }
    }

    private CostCalculator() { }

    static CostResult calculate(SkillingMethod method, int currentLevel, int currentXp,
        int targetLevel, int targetXp, boolean optional, boolean sell, boolean includeTax,
        int batchSize, PriceSnapshot snapshot, TaxSettings tax, Instant now)
    {
        int xpNeeded = Math.max(0, targetXp - currentXp);
        long actions = 0;
        long inputCost = 0;
        long outputGross = 0;
        long totalTax = 0;
        long net = 0;
        List<String> requiredLines = new ArrayList<>();
        List<String> optionalLines = new ArrayList<>();
        List<String> outputLines = new ArrayList<>();
        Context context = new Context(snapshot, now);
        CostResult.State state = CostResult.State.AVAILABLE;
        try
        {
            actions = method.actionsFor(xpNeeded);
            if (includeTax && sell && method.hasSellableOutputs() && !tax.valid())
            {
                state = CostResult.State.INVALID_SETTINGS;
                context.notices.add("Invalid GE tax settings. Check the plugin configuration.");
            }
            else
            {
                for (ItemQuantity input : method.getRequiredInputs())
                {
                    inputCost = Math.addExact(inputCost, inputLine(input, actions, batchSize, context, requiredLines));
                }
                if (optional)
                {
                    for (ItemQuantity input : method.getOptionalInputs())
                    {
                        inputCost = Math.addExact(inputCost, inputLine(input, actions, batchSize, context, optionalLines));
                    }
                }
                int multiplier = method.outputMultiplier(currentLevel);
                for (ItemQuantity output : method.getOutputs())
                {
                    long quantity = output.total(actions, batchSize, multiplier, true);
                    if (!sell || !output.isGePriced() || quantity == 0)
                    {
                        outputLines.add(String.format("%,d × %s", quantity, output.getName())
                            + (output.isGePriced() ? "\nNot sold." : "\nUntradeable; not GE-priced."));
                        continue;
                    }
                    WikiPrice.Quote quote = context.quote(output, false);
                    if (quote == null)
                    {
                        outputLines.add(String.format("%,d × %s\nSell quote unavailable.", quantity, output.getName()));
                        continue;
                    }
                    long total = Math.multiplyExact(quantity, quote.gp);
                    outputGross = Math.addExact(outputGross, total);
                    totalTax = Math.addExact(totalTax, includeTax && !output.isTaxExempt()
                        ? geTax(quote.gp, quantity, tax) : 0);
                    outputLines.add(Formats.itemLine(output.getName(), quantity, quote.gp, total)
                        + context.quoteNote(output, quote));
                }
                if (context.missing)
                {
                    state = CostResult.State.MISSING_QUOTES;
                }
                else
                {
                    net = Math.subtractExact(Math.subtractExact(outputGross, totalTax), inputCost);
                }
                if (method.hasLevelDependentOutput())
                {
                    context.notices.add("Output multiplier: " + multiplier + "× at starting level " + currentLevel
                        + "; held constant throughout this estimate.");
                }
            }
        }
        catch (ArithmeticException e)
        {
            state = CostResult.State.OUT_OF_RANGE;
            context.notices.add("Estimate exceeds the supported 64-bit quantity or GP range.");
        }
        return new CostResult(method, currentLevel, currentXp, targetLevel, targetXp, xpNeeded,
            actions, inputCost, outputGross, totalTax, net, requiredLines, optionalLines, outputLines,
            optional, sell, context.oldest, snapshot.fetchedAt, state, new ArrayList<>(context.notices));
    }


    static long geTax(long price, long quantity, TaxSettings tax)
    {
        if (!tax.valid() || price < 0 || quantity < 0)
        {
            throw new IllegalArgumentException("Invalid tax inputs");
        }
        if (price < tax.exemptBelowGp) { return 0; }
        long perItem = BigInteger.valueOf(price).multiply(BigInteger.valueOf(tax.basisPoints))
            .divide(BigInteger.valueOf(10_000)).min(BigInteger.valueOf(tax.capGp)).longValueExact();
        return Math.multiplyExact(perItem, quantity);
    }

    private static long inputLine(ItemQuantity item, long actions, int batchSize, Context context, List<String> lines)
    {
        long quantity = item.total(actions, batchSize, 1, false);
        if (quantity == 0)
        {
            lines.add("0 × " + item.getName());
            return 0;
        }
        if (item.getPricing() == ItemQuantity.Pricing.UNTRADEABLE)
        {
            lines.add(String.format("%,d × %s\nUntradeable; collect these supplies. Not GE-priced.", quantity, item.getName()));
            return 0;
        }
        if (item.hasFixedUnitPrice())
        {
            long total = Math.multiplyExact(quantity, item.getFixedUnitPrice());
            lines.add(Formats.itemLine(item.getName(), quantity, item.getFixedUnitPrice(), total) + "\nFixed fee.");
            return total;
        }
        WikiPrice.Quote quote = context.quote(item, true);
        if (quote == null)
        {
            lines.add(String.format("%,d × %s\nBuy quote unavailable.", quantity, item.getName()));
            return 0;
        }
        long total = Math.multiplyExact(quantity, quote.gp);
        lines.add(Formats.itemLine(item.getName(), quantity, quote.gp, total) + context.quoteNote(item, quote));
        return total;
    }

    private static final class Context
    {
        final PriceSnapshot snapshot;
        final Instant now;
        final Set<String> notices = new LinkedHashSet<>();
        boolean missing;
        long oldest;

        Context(PriceSnapshot snapshot, Instant now)
        {
            this.snapshot = snapshot;
            this.now = now;
        }

        WikiPrice.Quote quote(ItemQuantity item, boolean buy)
        {
            WikiPrice price = snapshot.prices.get(item.getItemId());
            WikiPrice.Quote quote = price == null ? null : buy ? price.buyQuote() : price.sellQuote();
            if (quote == null)
            {
                missing = true;
                notices.add("Quote unavailable: " + item.getName());
            }
            return quote;
        }

        String quoteNote(ItemQuantity item, WikiPrice.Quote quote)
        {
            String note = "\n" + quote.side + (quote.fallback ? " (fallback)" : "");
            if (quote.fallback)
            {
                notices.add("Opposite-side quote used: " + item.getName());
            }
            if (quote.timestamp == null || quote.timestamp > now.getEpochSecond())
            {
                notices.add("Quote age unknown: " + item.getName());
                return note + "; age unknown.";
            }
            oldest = oldest == 0 ? quote.timestamp : Math.min(oldest, quote.timestamp);
            long age = now.getEpochSecond() - quote.timestamp;
            if (age > 600)
            {
                notices.add("Quote older than ten minutes: " + item.getName());
                note += "; stale";
            }
            return note + "; " + Instant.ofEpochSecond(quote.timestamp) + ".";
        }
    }
}
