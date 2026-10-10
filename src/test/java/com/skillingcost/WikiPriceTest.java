package com.skillingcost;

import com.google.gson.Gson;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiPriceTest
{
    private final Gson gson = new Gson();

    @Test
    public void preservesLargePricesAndUnrelatedItems()
    {
        Map<Integer, WikiPrice> prices = WikiLatestResponse.parse(gson,
            "{\"data\":{\"1\":{\"high\":2450000000},\"2\":{\"low\":2149631130647},\"3\":{\"high\":17}}}");
        assertEquals(2_450_000_000L, prices.get(1).buyQuote().gp);
        assertEquals(2_149_631_130_647L, prices.get(2).sellQuote().gp);
        assertEquals(17L, prices.get(3).buyQuote().gp);
    }

    @Test
    public void isolatesInvalidSides()
    {
        for (String invalid : new String[]{"null", "0", "-1", "1.5", "\"bad\"", "9223372036854775808", "{}", "true"})
        {
            Map<Integer, WikiPrice> prices = WikiLatestResponse.parse(gson,
                "{\"data\":{\"1\":{\"high\":" + invalid + ",\"low\":9},\"2\":{\"high\":3},\"bad\":{}}}");
            assertTrue(prices.get(1).buyQuote().fallback);
            assertEquals(9L, prices.get(1).buyQuote().gp);
            assertEquals(3L, prices.get(2).buyQuote().gp);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidEnvelope()
    {
        WikiLatestResponse.parse(gson, "{\"data\":null}");
    }

    @Test
    public void usesTimestampForActualDirection()
    {
        WikiPrice price = new WikiPrice(20L, 10L, 500L, 100L);
        assertEquals(Long.valueOf(500), price.buyQuote().timestamp);
        assertEquals(Long.valueOf(100), price.sellQuote().timestamp);
        WikiPrice fallback = new WikiPrice(null, 10L, null, null);
        assertTrue(fallback.buyQuote().fallback);
        assertNull(fallback.buyQuote().timestamp);
        assertNull(new WikiPrice(null, null, null, null).buyQuote());
    }

    @Test
    public void formatsExactAndCompactSignedValues()
    {
        assertEquals("-9,223,372,036,854,775,808 gp", Formats.gp(Long.MIN_VALUE));
        assertEquals("2.15T gp", Formats.gpCompact(2_149_631_130_647L));
        assertEquals("-2.45B gp", Formats.gpCompact(-2_450_000_000L));
        assertEquals("1.0M gp", Formats.gpCompact(1_000_000));
        assertEquals("15K gp", Formats.gpCompact(15_000));
        assertEquals("950 gp", Formats.gpCompact(950));
        assertTrue(Formats.gpCompact(Long.MIN_VALUE).startsWith("-"));
        assertFalse(Formats.gpCompact(Long.MIN_VALUE).startsWith("--"));
        assertEquals("-0.33", Formats.gpPerXp(-1, 3));
        assertEquals("0.00", Formats.gpPerXp(0, 100));
        assertTrue(Formats.gpPerXp(Long.MIN_VALUE, 1).startsWith("-"));
        assertTrue(Formats.gpPerXp(Long.MAX_VALUE, 1).endsWith("T gp"));
    }
}
