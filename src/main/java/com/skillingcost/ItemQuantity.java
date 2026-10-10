package com.skillingcost;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Explicit pricing and exact consumption units for one resource. */
final class ItemQuantity
{
    enum Pricing { GE, FIXED_GP, UNTRADEABLE }

    private final int itemId;
    private final String name;
    private final BigDecimal quantityPerAction;
    private final Pricing pricing;
    private final long fixedUnitPrice;
    private final int unitsPerItem;
    private final boolean perBatch;
    private final boolean taxExempt;

    ItemQuantity(int itemId, String name, double quantity)
    {
        this(itemId, name, BigDecimal.valueOf(quantity), Pricing.GE, 0, 1, false, false);
    }

    ItemQuantity(int itemId, String name, double quantity, long fixedPrice)
    {
        this(itemId, name, BigDecimal.valueOf(quantity), Pricing.FIXED_GP, fixedPrice, 1, false, false);
    }

    ItemQuantity(int itemId, String name, BigDecimal quantity, Pricing pricing, long fixedPrice,
        int unitsPerItem, boolean perBatch, boolean taxExempt)
    {
        if (name == null || name.trim().isEmpty() || quantity == null || quantity.signum() < 0
            || pricing == null || unitsPerItem < 1 || fixedPrice < 0
            || (pricing != Pricing.FIXED_GP && itemId <= 0))
        {
            throw new IllegalArgumentException("Invalid resource: " + name);
        }
        this.itemId = itemId;
        this.name = name;
        this.quantityPerAction = quantity;
        this.pricing = pricing;
        this.fixedUnitPrice = fixedPrice;
        this.unitsPerItem = unitsPerItem;
        this.perBatch = perBatch;
        this.taxExempt = taxExempt;
    }

    int getItemId() { return itemId; }
    String getName() { return name; }
    BigDecimal getQuantityPerAction() { return quantityPerAction; }
    Pricing getPricing() { return pricing; }
    boolean isGePriced() { return pricing == Pricing.GE; }
    boolean hasFixedUnitPrice() { return pricing == Pricing.FIXED_GP; }
    long getFixedUnitPrice() { return fixedUnitPrice; }
    boolean isTaxExempt() { return taxExempt; }
    boolean isPerBatch() { return perBatch; }

    long total(long actions, int batchSize, int multiplier, boolean output)
    {
        if (actions < 0 || batchSize < 1 || multiplier < 1)
        {
            throw new IllegalArgumentException("Invalid action or batch count");
        }
        long units = perBatch ? ceilDivide(actions, batchSize) : actions;
        return quantityPerAction.multiply(BigDecimal.valueOf(units))
            .multiply(BigDecimal.valueOf(output ? multiplier : 1))
            .divide(BigDecimal.valueOf(unitsPerItem), 0, output ? RoundingMode.FLOOR : RoundingMode.CEILING)
            .longValueExact();
    }

    private static long ceilDivide(long value, int divisor)
    {
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }
}
