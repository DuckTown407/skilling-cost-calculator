package com.skillingcost;

/**
 * An item and how much of it one action consumes or produces.
 *
 * Quantities may be fractional (e.g. one jug of wine covering many libation offerings); the
 * total over a calculation is rounded per {@code roundUpTotal}. Items with a fixed unit price
 * (e.g. flat NPC fees, item id -1) skip the GE price lookup entirely.
 */
final class ItemQuantity
{
    private final int itemId;
    private final String name;
    private final double quantityPerAction;
    private final boolean roundUpTotal;
    private final int fixedUnitPrice;

    ItemQuantity(int itemId, String name, double quantityPerAction)
    {
        this(itemId, name, quantityPerAction, true);
    }

    ItemQuantity(int itemId, String name, double quantityPerAction, boolean roundUpTotal)
    {
        this(itemId, name, quantityPerAction, roundUpTotal, -1);
    }

    ItemQuantity(int itemId, String name, double quantityPerAction, boolean roundUpTotal, int fixedUnitPrice)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantityPerAction = quantityPerAction;
        this.roundUpTotal = roundUpTotal;
        this.fixedUnitPrice = fixedUnitPrice;
    }

    int getItemId()
    {
        return itemId;
    }

    String getName()
    {
        return name;
    }

    double getQuantityPerAction()
    {
        return quantityPerAction;
    }

    boolean hasFixedUnitPrice()
    {
        return fixedUnitPrice >= 0;
    }

    int getFixedUnitPrice()
    {
        return fixedUnitPrice;
    }

    long getTotalQuantity(long actions)
    {
        double raw = actions * quantityPerAction;
        if (roundUpTotal)
        {
            return (long) Math.ceil(raw);
        }
        return Math.round(raw);
    }
}
