package com.example.onboarding.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The demo profile seeds the partners that the README examples and http/requests.http rely on. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoProfileTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void seedsOnePartnerPerCountry() throws Exception {
        mvc.perform(get("/api/v1/business-partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].address.country").value(containsInAnyOrder("DE", "PL", "CH")));
    }

    @Test
    void readmeDuplicateExampleFindsTheSeededPartner() throws Exception {
        mvc.perform(post("/api/v1/business-partners/checks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "ACME Chemicals G.m.b.H.",
                         "address": {"street": "Industriestr. 1", "postalCode": "10115", "city": "Berlin", "country": "DE"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardable").value(false))
                .andExpect(jsonPath("$.duplicates[0].name").value("Acme Chemicals GmbH"))
                .andExpect(jsonPath("$.duplicates[0].reason").value("SIMILAR_NAME_AND_LOCATION"));
    }
}
