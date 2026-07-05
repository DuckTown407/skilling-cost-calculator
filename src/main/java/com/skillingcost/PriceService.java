package com.skillingcost;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
final class PriceService
{
    private static final String LATEST_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final Duration CACHE_DURATION = Duration.ofSeconds(60);

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final SkillingCostConfig config;
    private final Object lock = new Object();

    private Map<Integer, WikiPrice> cache = new HashMap<>();
    private Instant cacheTime = Instant.EPOCH;
    private Call activeCall;

    @Inject
    PriceService(OkHttpClient okHttpClient, Gson gson, SkillingCostConfig config)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
        this.config = config;
    }

    void getLatestPrices(Set<Integer> requiredItemIds, Consumer<Map<Integer, WikiPrice>> onSuccess, Consumer<String> onError)
    {
        if (requiredItemIds.isEmpty())
        {
            onSuccess.accept(new HashMap<>());
            return;
        }

        synchronized (lock)
        {
            if (isCacheUsable(requiredItemIds))
            {
                onSuccess.accept(copyRequiredPrices(requiredItemIds));
                return;
            }
        }

        Request request = new Request.Builder()
            .url(LATEST_URL)
            .header("User-Agent", wikiUserAgent())
            .build();

        Call call = okHttpClient.newCall(request);
        synchronized (lock)
        {
            activeCall = call;
        }

        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                if (!call.isCanceled())
                {
                    onError.accept("Price fetch failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (!response.isSuccessful())
                    {
                        onError.accept("Price fetch failed: HTTP " + response.code());
                        return;
                    }

                    ResponseBody body = response.body();
                    if (body == null)
                    {
                        onError.accept("Price fetch failed: empty response");
                        return;
                    }

                    WikiLatestResponse latest = gson.fromJson(body.string(), WikiLatestResponse.class);
                    if (latest == null || latest.data == null)
                    {
                        onError.accept("Price fetch failed: invalid response");
                        return;
                    }

                    Map<Integer, WikiPrice> parsed = new HashMap<>();
                    for (Map.Entry<String, WikiPrice> entry : latest.data.entrySet())
                    {
                        try
                        {
                            parsed.put(Integer.parseInt(entry.getKey()), entry.getValue());
                        }
                        catch (NumberFormatException ignoredKey)
                        {
                            // Ignore malformed API keys.
                        }
                    }

                    synchronized (lock)
                    {
                        cache = parsed;
                        cacheTime = Instant.now();
                        activeCall = null;
                    }

                    Map<Integer, WikiPrice> required = copyRequiredPrices(requiredItemIds);
                    onSuccess.accept(required);
                }
                catch (Exception e)
                {
                    onError.accept("Price fetch failed: " + e.getMessage());
                }
            }
        });
    }

    void cancel()
    {
        synchronized (lock)
        {
            if (activeCall != null)
            {
                activeCall.cancel();
                activeCall = null;
            }
        }
    }

    private String wikiUserAgent()
    {
        String userAgent = config.wikiUserAgent();
        if (userAgent == null || userAgent.trim().isEmpty())
        {
            return "skilling-cost-calculator RuneLite plugin";
        }
        return userAgent.trim();
    }

    private boolean isCacheUsable(Set<Integer> requiredItemIds)
    {
        return !cache.isEmpty()
            && Duration.between(cacheTime, Instant.now()).compareTo(CACHE_DURATION) < 0
            && cache.keySet().containsAll(requiredItemIds);
    }

    private Map<Integer, WikiPrice> copyRequiredPrices(Set<Integer> requiredItemIds)
    {
        synchronized (lock)
        {
            Map<Integer, WikiPrice> out = new HashMap<>();
            for (Integer itemId : requiredItemIds)
            {
                WikiPrice price = cache.get(itemId);
                if (price != null)
                {
                    out.put(itemId, price);
                }
            }
            return out;
        }
    }
}
