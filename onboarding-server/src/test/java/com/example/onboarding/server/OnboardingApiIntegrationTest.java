package com.example.onboarding.server;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through the real Spring context: web adapter -> use case -> library -> persistence adapter. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OnboardingApiIntegrationTest {

    private static final String ACME = """
            {"name": "Acme Chemicals GmbH",
             "address": {"street": "Industriestraße 1", "postalCode": "10115", "city": "Berlin", "country": "DE"},
             "vatId": "DE 136 695 976", "iban": "DE89 3704 0044 0532 0130 00"}
            """;

    private static final String ACME_SPELLING_VARIANT = """
            {"name": "ACME Chemicals G.m.b.H.",
             "address": {"street": "Industriestr. 1", "postalCode": "10115", "city": "Berlin", "country": "DE"}}
            """;

    private static final String BROKEN = """
            {"name": "Broken Corp",
             "address": {"city": "Berlin", "postalCode": "1234", "country": "DE"},
             "vatId": "DE136695977", "iban": "DE89370400440532013001"}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void onboardThenFetchThenRejectDuplicate() throws Exception {
        String created = mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(ACME))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partner.vatId").value("DE136695976"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.partner.id");

        mvc.perform(get("/api/v1/business-partners/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Chemicals GmbH"));

        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(ACME_SPELLING_VARIANT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.duplicates[0].partnerId").value(id));

        mvc.perform(get("/api/v1/business-partners").param("country", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void reportsEveryProblemAtOnce() throws Exception {
        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(BROKEN))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[?(@.ruleId == 'POSTAL_CODE_FORMAT')]").exists())
                .andExpect(jsonPath("$.violations[?(@.ruleId == 'VAT_ID')]").exists())
                .andExpect(jsonPath("$.violations[?(@.ruleId == 'IBAN')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'address.street')].severity").value("WARNING"));
    }

    @Test
    void checkEndpointDoesNotStore() throws Exception {
        mvc.perform(post("/api/v1/business-partners/checks").contentType(MediaType.APPLICATION_JSON).content(ACME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardable").value(true));

        mvc.perform(get("/api/v1/business-partners"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void malformedJsonIsAProblemResponse() throws Exception {
        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exposesOpenApiAndHealth() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/business-partners']").exists());
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
