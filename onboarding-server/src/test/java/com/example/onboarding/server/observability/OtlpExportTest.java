package com.example.onboarding.server.observability;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the otel profile, spans are exported over OTLP/HTTP. A minimal OTLP receiver (the JDK's HTTP server) stands in
 * for an OpenTelemetry Collector, so the test needs no infrastructure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("otel")
@DirtiesContext
class OtlpExportTest {

    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();
    private static final HttpServer COLLECTOR = startCollector();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SdkTracerProvider tracerProvider;

    @DynamicPropertySource
    static void collectorEndpoint(DynamicPropertyRegistry registry) {
        String endpoint = "http://localhost:" + COLLECTOR.getAddress().getPort();
        registry.add("management.otlp.tracing.endpoint", () -> endpoint + "/v1/traces");
        registry.add("management.otlp.metrics.export.url", () -> endpoint + "/v1/metrics");
    }

    @AfterAll
    static void stopCollector() {
        COLLECTOR.stop(0);
    }

    @Test
    void exportsRequestSpansToTheCollector() throws Exception {
        mvc.perform(get("/api/v1/business-partners")).andExpect(status().isOk());

        CompletableResultCode flushed = tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

        assertThat(flushed.isSuccess()).isTrue();
        assertThat(RECEIVED).contains("POST /v1/traces application/x-protobuf");
    }

    private static HttpServer startCollector() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                RECEIVED.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
                        + exchange.getRequestHeaders().getFirst("Content-Type"));
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
