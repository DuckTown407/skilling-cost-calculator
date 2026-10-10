package com.skillingcost;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Parse independently so one invalid or expensive item cannot break the feed. */
final class WikiLatestResponse
{
    private WikiLatestResponse()
    {
    }

    static Map<Integer, WikiPrice> parse(Gson gson, String json)
    {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        if (root == null || !root.has("data") || !root.get("data").isJsonObject())
        {
            throw new IllegalArgumentException("Invalid price response");
        }
        Map<Integer, WikiPrice> prices = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("data").entrySet())
        {
            try
            {
                int id = Integer.parseInt(entry.getKey());
                if (id <= 0 || !entry.getValue().isJsonObject())
                {
                    continue;
                }
                JsonObject item = entry.getValue().getAsJsonObject();
                prices.put(id, new WikiPrice(number(item.get("high")), number(item.get("low")),
                    number(item.get("highTime")), number(item.get("lowTime"))));
            }
            catch (NumberFormatException ignored)
            {
                // Non-item keys do not invalidate other quotes.
            }
        }
        return prices;
    }

    private static Long number(JsonElement value)
    {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
        {
            return null;
        }
        try
        {
            long parsed = new BigDecimal(value.getAsString()).longValueExact();
            return parsed > 0 ? parsed : null;
        }
        catch (ArithmeticException | NumberFormatException ignored)
        {
            return null;
        }
    }
}
