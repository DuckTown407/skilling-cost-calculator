package com.skillingcost;

final class WikiPrice
{
    private Integer high;
    private Integer low;
    private Long highTime;
    private Long lowTime;

    int getInstantBuyPrice()
    {
        if (high != null && high > 0)
        {
            return high;
        }
        return low != null && low > 0 ? low : 0;
    }

    int getInstantSellPrice()
    {
        if (low != null && low > 0)
        {
            return low;
        }
        return high != null && high > 0 ? high : 0;
    }

    long getNewestTimestamp()
    {
        long hi = highTime == null ? 0 : highTime;
        long lo = lowTime == null ? 0 : lowTime;
        return Math.max(hi, lo);
    }
}
