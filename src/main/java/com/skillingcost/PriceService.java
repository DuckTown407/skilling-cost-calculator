package com.skillingcost;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Singleton
final class PriceService
{
    private static final String LATEST_URL = "https://prices.runescape.wiki/api/v2/osrs/latest";
    private static final Duration CACHE_DURATION = Duration.ofSeconds(60);

    private final Call.Factory calls;
    private final Gson gson;
    private final SkillingCostConfig config;
    private final Clock clock;
    private final Object lock = new Object();
    private final List<WaitingConsumer> waiting = new ArrayList<>();

    private PriceSnapshot cache;
    private Call activeCall;
    private long generation;

    @Inject
    PriceService(OkHttpClient client, Gson gson, SkillingCostConfig config)
    {
        this(client, gson, config, Clock.systemUTC());
    }

    PriceService(Call.Factory calls, Gson gson, SkillingCostConfig config, Clock clock)
    {
        this.calls = calls;
        this.gson = gson;
        this.config = config;
        this.clock = clock;
    }

    void getLatestSnapshot(Set<Integer> ids, boolean refresh, Consumer<PriceSnapshot> success, Consumer<String> error)
    {
        if (ids.isEmpty())
        {
            success.accept(new PriceSnapshot(Collections.emptyMap(), Instant.EPOCH));
            return;
        }
        if (!config.enableWikiPrices())
        {
            error.accept("Enable Wiki prices in plugin config to price GE resources.");
            return;
        }
        WaitingConsumer consumer = new WaitingConsumer(ids, success, error);
        PriceSnapshot cached = null;
        Call started = null;
        long requestGeneration;
        synchronized (lock)
        {
            requestGeneration = generation;
            Duration age = cache == null ? CACHE_DURATION : Duration.between(cache.fetchedAt, clock.instant());
            if (!refresh && cache != null && !age.isNegative() && age.compareTo(CACHE_DURATION) < 0)
            {
                cached = subset(cache, ids);
            }
            else
            {
                waiting.add(consumer);
                if (activeCall == null)
                {
                    try
                    {
                        activeCall = calls.newCall(new Request.Builder().url(LATEST_URL)
                            .header("User-Agent", wikiUserAgent()).build());
                        started = activeCall;
                    }
                    catch (RuntimeException e)
                    {
                        waiting.remove(consumer);
                        log.debug("Cannot prepare price request", e);
                    }
                }
            }
        }
        if (cached != null)
        {
            if (isCurrent(requestGeneration)) { success.accept(cached); }
            return;
        }
        if (started == null)
        {
            synchronized (lock)
            {
                if (activeCall != null || generation != requestGeneration) { return; }
            }
            error.accept("Unable to prepare price request. Check the Wiki User-Agent setting.");
            return;
        }
        final long token = requestGeneration;
        final Call call = started;
        Callback callback = new Callback()
        {
            @Override
            public void onFailure(Call failed, IOException exception)
            {
                log.debug("Price acquisition failed", exception);
                complete(call, token, null, "Could not reach Wiki prices. Try refreshing.");
            }

            @Override
            public void onResponse(Call responded, Response response)
            {
                PriceSnapshot snapshot = null;
                String failure = null;
                try (Response ignored = response)
                {
                    if (!response.isSuccessful())
                    {
                        failure = "Wiki price request failed (HTTP " + response.code() + ").";
                    }
                    else
                    {
                        ResponseBody body = response.body();
                        if (body == null)
                        {
                            failure = "Wiki prices returned an empty response.";
                        }
                        else
                        {
                            snapshot = new PriceSnapshot(WikiLatestResponse.parse(gson, body.string()), clock.instant());
                        }
                    }
                }
                catch (IOException | RuntimeException exception)
                {
                    log.debug("Cannot read Wiki price response", exception);
                    failure = "Wiki prices returned an unreadable response. Try refreshing.";
                }
                complete(call, token, snapshot, failure);
            }
        };
        try
        {
            call.enqueue(callback);
        }
        catch (RuntimeException exception)
        {
            log.debug("Cannot queue price request", exception);
            complete(call, token, null, "Unable to start the Wiki price request. Try refreshing.");
        }
    }

    private void complete(Call call, long token, PriceSnapshot snapshot, String error)
    {
        List<WaitingConsumer> consumers;
        synchronized (lock)
        {
            if (activeCall != call || generation != token) { return; }
            activeCall = null;
            consumers = new ArrayList<>(waiting);
            waiting.clear();
            if (snapshot != null) { cache = snapshot; }
        }
        for (WaitingConsumer consumer : consumers)
        {
            if (!isCurrent(token)) { return; }
            try
            {
                if (snapshot != null) { consumer.success.accept(subset(snapshot, consumer.ids)); }
                else { consumer.error.accept(error); }
            }
            catch (RuntimeException exception)
            {
                log.debug("Price consumer failed", exception);
            }
        }
    }

    void cancel()
    {
        Call cancel;
        synchronized (lock)
        {
            generation++;
            cancel = activeCall;
            activeCall = null;
            waiting.clear();
            cache = null;
        }
        if (cancel != null) { cancel.cancel(); }
    }

    private boolean isCurrent(long token)
    {
        synchronized (lock)
        {
            return generation == token && config.enableWikiPrices();
        }
    }

    private static PriceSnapshot subset(PriceSnapshot snapshot, Set<Integer> ids)
    {
        Map<Integer, WikiPrice> prices = new HashMap<>();
        for (Integer id : ids)
        {
            WikiPrice quote = snapshot.prices.get(id);
            if (quote != null) { prices.put(id, quote); }
        }
        return new PriceSnapshot(prices, snapshot.fetchedAt);
    }

    private String wikiUserAgent()
    {
        String agent = config.wikiUserAgent();
        if (agent == null || agent.trim().isEmpty()
            || "skilling-cost-calculator RuneLite plugin".equals(agent.trim()))
        {
            return SkillingCostConfig.DEFAULT_WIKI_USER_AGENT;
        }
        return agent.trim();
    }

    private static final class WaitingConsumer
    {
        final Set<Integer> ids;
        final Consumer<PriceSnapshot> success;
        final Consumer<String> error;

        WaitingConsumer(Set<Integer> ids, Consumer<PriceSnapshot> success, Consumer<String> error)
        {
            this.ids = Collections.unmodifiableSet(new java.util.LinkedHashSet<>(ids));
            this.success = success;
            this.error = error;
        }
    }
}
