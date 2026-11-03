package com.example.onboarding.server.observability;

import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actuator endpoints and OpenTelemetry tracing as configured in application.yml (no OTLP export by default). */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@DirtiesContext
class ObservabilityTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TraceIdsSeenByListeners seen;

    @Test
    void exposesKubernetesProbes() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoShowsBuildAndRuntime() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.artifact").value("onboarding-server"))
                .andExpect(jsonPath("$.build.time").doesNotExist())
                .andExpect(jsonPath("$.java.version").exists())
                .andExpect(jsonPath("$.os.name").exists());
    }

    @Test
    void prometheusEndpointHasHttpMetricsTaggedWithTheApplication() throws Exception {
        mvc.perform(get("/api/v1/business-partners")).andExpect(status().isOk());

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests_seconds_count{application=\"partner-onboarding\"")));
    }

    @Test
    void tracerIsOpenTelemetryAndNothingIsExportedByDefault() {
        assertThat(context.getBean(Tracer.class)).isInstanceOf(OtelTracer.class);
        assertThat(context.getBeanNamesForType(OtlpHttpSpanExporter.class)).isEmpty();
    }

    @Test
    void continuesAnIncomingW3cTraceAndPutsItsIdIntoTheLogContext() throws Exception {
        mvc.perform(post("/api/v1/business-partners")
                        .header("traceparent", "00-" + TRACE_ID + "-00f067aa0ba902b7-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Traced Sp. z o.o.",
                                 "address": {"street": "ul. Testowa 9", "postalCode": "30-009", "city": "Kraków", "country": "PL"}}
                                """))
                .andExpect(status().isCreated());

        assertThat(seen.traceIds).containsExactly(TRACE_ID);
    }

    /** Records the MDC trace id the audit listener's log line would carry. */
    @TestConfiguration
    static class Config {
        @Bean
        TraceIdsSeenByListeners traceIdsSeenByListeners() {
            return new TraceIdsSeenByListeners();
        }
    }

    static class TraceIdsSeenByListeners {
        final List<String> traceIds = new ArrayList<>();

        @EventListener
        void on(BusinessPartnerOnboarded event) {
            traceIds.add(MDC.get("traceId"));
        }
    }
}
