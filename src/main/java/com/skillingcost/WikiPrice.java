package com.skillingcost;

/** Immutable trade quotes; absence is distinct from a zero GP valuation. */
final class WikiPrice
{
    private final Long high;
    private final Long low;
    private final Long highTime;
    private final Long lowTime;

    WikiPrice(Long high, Long low, Long highTime, Long lowTime)
    {
        this.high = positive(high);
        this.low = positive(low);
        this.highTime = positive(highTime);
        this.lowTime = positive(lowTime);
    }

    Quote buyQuote()
    {
        return high != null ? new Quote(high, highTime, false, "instant buy")
            : low != null ? new Quote(low, lowTime, true, "instant sell") : null;
    }

    Quote sellQuote()
    {
        return low != null ? new Quote(low, lowTime, false, "instant sell")
            : high != null ? new Quote(high, highTime, true, "instant buy") : null;
    }

    private static Long positive(Long value)
    {
        return value != null && value > 0 ? value : null;
    }

    static final class Quote
    {
        final long gp;
        final Long timestamp;
        final boolean fallback;
        final String side;

        Quote(long gp, Long timestamp, boolean fallback, String side)
        {
            this.gp = gp;
            this.timestamp = timestamp;
            this.fallback = fallback;
            this.side = side;
        }
    }
}
