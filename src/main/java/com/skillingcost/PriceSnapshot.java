package com.skillingcost;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class PriceSnapshot
{
    final Map<Integer, WikiPrice> prices;
    final Instant fetchedAt;

    PriceSnapshot(Map<Integer, WikiPrice> prices, Instant fetchedAt)
    {
        this.prices = Collections.unmodifiableMap(new HashMap<>(prices));
        this.fetchedAt = fetchedAt;
    }
}
