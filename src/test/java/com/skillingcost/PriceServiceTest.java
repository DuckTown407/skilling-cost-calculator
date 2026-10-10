package com.skillingcost;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Timeout;
import org.junit.Test;
import static org.junit.Assert.*;

public class PriceServiceTest
{
    private final FakeClock clock = new FakeClock();
    private final FakeCalls calls = new FakeCalls();
    private final TestConfig config = new TestConfig();
    private final PriceService service = new PriceService(calls, new Gson(), config, clock);

    @Test
    public void sharesRequestsCachesAbsenceAndInvokesOutsideLock() throws Exception
    {
        List<PriceSnapshot> received = new ArrayList<>();
        service.getLatestSnapshot(Collections.singleton(1), false, snapshot -> {
            received.add(snapshot);
            service.getLatestSnapshot(Collections.singleton(3), false, received::add, this::fail);
        }, this::fail);
        service.getLatestSnapshot(Collections.singleton(2), false, received::add, this::fail);
        assertEquals(1, calls.list.size());
        calls.list.get(0).respond(200, "{\"data\":{\"1\":{\"high\":2450000000}}}");
        assertEquals(3, received.size());
        assertEquals(1, calls.list.size());
        assertTrue(received.get(1).prices.isEmpty());
        assertEquals(clock.instant(), received.get(0).fetchedAt);
        clock.seconds += 59;
        service.getLatestSnapshot(Collections.singleton(2), false, received::add, this::fail);
        assertEquals(1, calls.list.size());
        clock.seconds++;
        service.getLatestSnapshot(Collections.singleton(2), false, received::add, this::fail);
        assertEquals(2, calls.list.size());
        calls.list.get(1).respond(200, "{\"data\":{}}");
        service.getLatestSnapshot(Collections.singleton(2), false, received::add, this::fail);
        assertEquals(2, calls.list.size());
    }

    @Test
    public void refreshJoinsInflightAndCancellationRejectsOldCompletion() throws Exception
    {
        AtomicInteger received = new AtomicInteger();
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), this::fail);
        FakeCall old = calls.list.get(0);
        service.getLatestSnapshot(Collections.singleton(2), true, x -> received.incrementAndGet(), this::fail);
        assertEquals(1, calls.list.size());
        service.cancel();
        assertTrue(old.isCanceled());
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), this::fail);
        old.respond(200, "{\"data\":{}}");
        assertEquals(0, received.get());
        service.getLatestSnapshot(Collections.singleton(2), true, x -> received.incrementAndGet(), this::fail);
        assertEquals(2, calls.list.size());
        calls.list.get(1).respond(200, "{\"data\":{}}");
        assertEquals(2, received.get());
        service.getLatestSnapshot(Collections.singleton(1), true, x -> received.incrementAndGet(), this::fail);
        assertEquals(3, calls.list.size());
    }

    @Test
    public void disabledPricesAndNoQuoteWorkSendNoRequests()
    {
        config.enabled = false;
        List<String> errors = new ArrayList<>();
        service.getLatestSnapshot(Collections.singleton(1), false, x -> fail("unexpected success"), errors::add);
        AtomicInteger received = new AtomicInteger();
        service.getLatestSnapshot(Collections.emptySet(), true, x -> received.incrementAndGet(), errors::add);
        assertEquals(1, errors.size());
        assertEquals(1, received.get());
        assertTrue(calls.list.isEmpty());
    }

    @Test
    public void errorsDoNotRefreshOldSnapshot() throws Exception
    {
        List<String> errors = new ArrayList<>();
        service.getLatestSnapshot(Collections.singleton(1), false, x -> {}, errors::add);
        calls.list.get(0).respond(200, "{\"data\":{}}");
        clock.seconds += 60;
        service.getLatestSnapshot(Collections.singleton(1), false, x -> fail("unexpected success"), errors::add);
        calls.list.get(1).respond(500, "error");
        service.getLatestSnapshot(Collections.singleton(1), false, x -> fail("unexpected success"), errors::add);
        assertEquals(3, calls.list.size());
        calls.list.get(2).respond(200, "{\"data\":null}");
        service.getLatestSnapshot(Collections.singleton(1), false, x -> fail("unexpected success"), errors::add);
        calls.list.get(3).callback.onFailure(calls.list.get(3), new IOException("raw internal detail"));
        assertEquals(3, errors.size());
        assertFalse(String.join(" ", errors).contains("raw internal detail"));
    }

    private void fail(String error) { org.junit.Assert.fail(error); }

    @Test
    public void disableReenableAndEnqueueFailureReleaseAcquisition() throws Exception
    {
        List<String> errors = new ArrayList<>();
        AtomicInteger received = new AtomicInteger();
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), errors::add);
        FakeCall obsolete = calls.list.get(0);
        config.enabled = false;
        service.cancel();
        obsolete.respond(200, "{\"data\":{}}");
        assertEquals(0, received.get());
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), errors::add);
        assertEquals(1, calls.list.size());
        config.enabled = true;
        calls.failEnqueue = true;
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), errors::add);
        assertEquals(2, errors.size());
        calls.failEnqueue = false;
        service.getLatestSnapshot(Collections.singleton(1), false, x -> received.incrementAndGet(), errors::add);
        calls.list.get(2).respond(200, "{\"data\":{}}");
        assertEquals(1, received.get());
        assertEquals(3, calls.list.size());
    }

    private static final class TestConfig implements SkillingCostConfig
    {
        boolean enabled = true;
        @Override public boolean enableWikiPrices() { return enabled; }
    }

    private static final class FakeClock extends Clock
    {
        long seconds = 1000;
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochSecond(seconds); }
    }

    private static final class FakeCalls implements Call.Factory
    {
        final List<FakeCall> list = new ArrayList<>();
        boolean failEnqueue;
        @Override public Call newCall(Request request)
        {
            FakeCall call = new FakeCall(request);
            call.failEnqueue = failEnqueue;
            list.add(call);
            return call;
        }
    }

    private static final class FakeCall implements Call
    {
        final Request request;
        Callback callback;
        boolean canceled;
        boolean failEnqueue;
        FakeCall(Request request) { this.request = request; }
        void respond(int code, String body) throws IOException
        {
            callback.onResponse(this, new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(code).message("fixture").body(ResponseBody.create(MediaType.parse("application/json"), body)).build());
        }
        @Override public Request request() { return request; }
        @Override public Response execute() { throw new UnsupportedOperationException(); }
        @Override public void enqueue(Callback callback)
        {
            if (failEnqueue) { throw new IllegalStateException("fixture queue failure"); }
            this.callback = callback;
        }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isExecuted() { return callback != null; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public Timeout timeout() { return Timeout.NONE; }
        @Override public Call clone() { return new FakeCall(request); }
    }
}
