package com.mbed.coap.metrics.micrometer;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.utils.FutureHelpers.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.utils.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.validateMockitoUsage;

class MicrometerMetricsFilterTest {
    private MeterRegistry registry = new LoggingMeterRegistry();
    private MicrometerMetricsFilter filter = MicrometerMetricsFilter.builder().registry(registry).build();
    private Service<CoapRequest, CoapResponse> okService = filter.then(__ -> completedFuture(ok("OK")));
    private Service<CoapRequest, CoapResponse> failingService = filter.then(__ -> failedFuture(new Exception("error message")));

    @AfterEach
    public void validate() {
        validateMockitoUsage();
    }

    @Test
    public void shouldForwardResponse() throws ExecutionException, InterruptedException {
        CoapResponse resp = okService.apply(get("/test/1")).get();

        assertEquals(resp, ok("OK"));
    }

    @Test
    public void shouldRegisterTimerMetric() throws ExecutionException, InterruptedException {
        okService.apply(get("/test/1")).join();
        assertNotNull(
                registry.find("coap.server.requests")
                        .tag("route", "/test/1")
                        .tag("status", "205")
                        .tag("method", "GET")
                        .tag("throwable", "n/a")
                        .timer()
        );

        assertThrows(Exception.class, () -> failingService.apply(get("/test/2")).get());
        assertNotNull(
                registry.find("coap.server.requests")
                        .tag("route", "/test/2")
                        .tag("status", "n/a")
                        .tag("method", "GET")
                        .tag("throwable", "java.lang.Exception")
                        .timer()
        );
    }
}
