package com.skillingcost;

final class ItemQuantity
{
    private final int itemId;
    private final String name;
    private final double quantityPerAction;
    private final boolean roundUpTotal;

    ItemQuantity(int itemId, String name, double quantityPerAction)
    {
        this(itemId, name, quantityPerAction, true);
    }

    ItemQuantity(int itemId, String name, double quantityPerAction, boolean roundUpTotal)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantityPerAction = quantityPerAction;
        this.roundUpTotal = roundUpTotal;
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
