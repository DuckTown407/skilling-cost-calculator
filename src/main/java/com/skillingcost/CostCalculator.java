package com.skillingcost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure cost math: given a method, an action count, and a price map, computes input costs,
 * output revenue, GE tax, and net gp. No I/O and no Swing, so it is easy to unit test.
 *
 * Missing prices (untradeable or unfetched items) are treated as 0 gp rather than failing.
 */
final class CostCalculator
{
    /** GE tax parameters, snapshotted from config at calculation time. */
    static final class TaxSettings
    {
        private final int basisPoints;
        private final int capGp;
        private final int exemptBelowGp;

        TaxSettings(int basisPoints, int capGp, int exemptBelowGp)
        {
            this.basisPoints = basisPoints;
            this.capGp = capGp;
            this.exemptBelowGp = exemptBelowGp;
        }
    }

    private CostCalculator()
    {
    }

    /** Net gp (sell revenue - tax - input cost) without building display lines. */
    static long estimateNet(
        SkillingMethod method,
        long actionsNeeded,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax,
        Map<Integer, WikiPrice> prices,
        TaxSettings tax)
    {
        long inputCost = 0;
        for (ItemQuantity input : method.getRequiredInputs())
        {
            inputCost += buyTotal(input, actionsNeeded, prices);
        }
        if (includeOptionalInputs)
        {
            for (ItemQuantity input : method.getOptionalInputs())
            {
                inputCost += buyTotal(input, actionsNeeded, prices);
            }
        }

        long outputGross = 0;
        long taxTotal = 0;
        if (sellOutputs)
        {
            for (ItemQuantity output : method.getOutputs())
            {
                int unitSell = sellUnitPrice(output, prices);
                long quantity = output.getTotalQuantity(actionsNeeded);
                outputGross += quantity * unitSell;
                taxTotal += includeTax ? geTax(unitSell, quantity, tax) : 0;
            }
        }

        return outputGross - taxTotal - inputCost;
    }

    /** Full breakdown with per-item display lines for the result panel. */
    static CostResult buildResult(
        SkillingMethod method,
        int currentLevel,
        int currentXp,
        int targetLevel,
        int targetXp,
        int xpNeeded,
        long actionsNeeded,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax,
        Map<Integer, WikiPrice> prices,
        TaxSettings tax)
    {
        long inputCost = 0;
        long outputGross = 0;
        long taxTotal = 0;
        long newestPriceTimestamp = 0;
        List<String> requiredInputLines = new ArrayList<>();
        List<String> optionalInputLines = new ArrayList<>();
        List<String> outputLines = new ArrayList<>();

        for (ItemQuantity input : method.getRequiredInputs())
        {
            inputCost += appendInputLine(input, actionsNeeded, prices, requiredInputLines, false);
            newestPriceTimestamp = Math.max(newestPriceTimestamp, priceTimestamp(input, prices));
        }
        if (includeOptionalInputs)
        {
            for (ItemQuantity input : method.getOptionalInputs())
            {
                inputCost += appendInputLine(input, actionsNeeded, prices, optionalInputLines, true);
                newestPriceTimestamp = Math.max(newestPriceTimestamp, priceTimestamp(input, prices));
            }
        }

        if (sellOutputs)
        {
            for (ItemQuantity output : method.getOutputs())
            {
                int unitSell = sellUnitPrice(output, prices);
                long quantity = output.getTotalQuantity(actionsNeeded);
                long lineTotal = quantity * unitSell;
                outputGross += lineTotal;
                taxTotal += includeTax ? geTax(unitSell, quantity, tax) : 0;
                outputLines.add(Formats.itemLine(output.getName(), quantity, unitSell, lineTotal));
                newestPriceTimestamp = Math.max(newestPriceTimestamp, priceTimestamp(output, prices));
            }
        }

        long net = outputGross - taxTotal - inputCost;

        return new CostResult(
            method,
            currentLevel,
            currentXp,
            targetLevel,
            targetXp,
            xpNeeded,
            actionsNeeded,
            inputCost,
            outputGross,
            taxTotal,
            net,
            requiredInputLines,
            optionalInputLines,
            outputLines,
            includeOptionalInputs,
            sellOutputs,
            newestPriceTimestamp);
    }

    /**
     * GE tax for one output line. Items whose unit price is below the exemption threshold are
     * tax free; otherwise tax is {@code basisPoints/10000} of the price, capped per item.
     */
    static long geTax(int unitSellPrice, long quantity, TaxSettings tax)
    {
        if (unitSellPrice < tax.exemptBelowGp)
        {
            return 0;
        }
        long taxPerItem = ((long) unitSellPrice * tax.basisPoints) / 10_000L;
        taxPerItem = Math.min(taxPerItem, tax.capGp);
        return taxPerItem * quantity;
    }

    private static long appendInputLine(
        ItemQuantity item,
        long actionsNeeded,
        Map<Integer, WikiPrice> prices,
        List<String> lines,
        boolean optional)
    {
        int unitBuy = buyUnitPrice(item, prices);
        long quantity = item.getTotalQuantity(actionsNeeded);
        long total = quantity * unitBuy;
        String name = optional ? item.getName() + " (optional)" : item.getName();
        lines.add(Formats.itemLine(name, quantity, unitBuy, total));
        return total;
    }

    private static long buyTotal(ItemQuantity item, long actionsNeeded, Map<Integer, WikiPrice> prices)
    {
        return item.getTotalQuantity(actionsNeeded) * (long) buyUnitPrice(item, prices);
    }

    private static int buyUnitPrice(ItemQuantity item, Map<Integer, WikiPrice> prices)
    {
        if (item.hasFixedUnitPrice())
        {
            return item.getFixedUnitPrice();
        }
        WikiPrice price = prices.get(item.getItemId());
        return price == null ? 0 : price.getInstantBuyPrice();
    }

    private static int sellUnitPrice(ItemQuantity item, Map<Integer, WikiPrice> prices)
    {
        if (item.hasFixedUnitPrice())
        {
            return item.getFixedUnitPrice();
        }
        WikiPrice price = prices.get(item.getItemId());
        return price == null ? 0 : price.getInstantSellPrice();
    }

    private static long priceTimestamp(ItemQuantity item, Map<Integer, WikiPrice> prices)
    {
        if (item.hasFixedUnitPrice())
        {
            return 0;
        }
        WikiPrice price = prices.get(item.getItemId());
        return price == null ? 0 : price.getNewestTimestamp();
    }
}
