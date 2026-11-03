package com.example.onboarding.server.adapter.in.web;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.port.in.CheckPartnerUseCase;
import com.example.onboarding.application.port.in.FindPartnersQuery;
import com.example.onboarding.application.port.in.OnboardPartnerUseCase;
import com.example.onboarding.application.port.in.OnboardingResult.DuplicateSuspected;
import com.example.onboarding.application.port.in.OnboardingResult.Onboarded;
import com.example.onboarding.application.port.in.OnboardingResult.Rejected;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.matching.MatchReason;
import com.example.partnerquality.testing.PartnerFixtures;
import com.example.partnerquality.validation.RuleViolation;
import com.example.partnerquality.validation.ValidationReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice test of the driving adapter. The incoming ports are mocked: this verifies the HTTP contract
 * (status codes, headers, JSON shape) for each use-case outcome, nothing else.
 */
@WebMvcTest(BusinessPartnerController.class)
class BusinessPartnerControllerTest {

    private static final String BODY = """
            {"name": "Acme Chemicals GmbH",
             "address": {"street": "Industriestraße 1", "postalCode": "10115", "city": "Berlin", "country": "DE"},
             "vatId": "DE136695976"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OnboardPartnerUseCase onboardPartner;
    @MockitoBean
    private CheckPartnerUseCase checkPartner;
    @MockitoBean
    private FindPartnersQuery findPartners;

    private final BusinessPartner acme = BusinessPartner.onboard(PartnerFixtures.acme(), Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void returns201WithLocationWhenOnboarded() throws Exception {
        when(onboardPartner.onboard(any())).thenReturn(new Onboarded(acme, List.of(), List.of()));

        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/business-partners/" + acme.id()))
                .andExpect(jsonPath("$.partner.name").value("Acme Chemicals GmbH"));
    }

    @Test
    void returns422ProblemWhenRulesFail() throws Exception {
        RuleViolation violation = RuleViolation.error("IBAN", "iban", "IBAN has invalid check digits");
        when(onboardPartner.onboard(any())).thenReturn(new Rejected(new ValidationReport(List.of(violation))));

        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.type").value("urn:problem:partner-onboarding:validation-failed"))
                .andExpect(jsonPath("$.violations[0].ruleId").value("IBAN"));
    }

    @Test
    void returns409ProblemWhenDuplicate() throws Exception {
        when(onboardPartner.onboard(any()))
                .thenReturn(new DuplicateSuspected(List.of(new Match<>(acme, 1.0, MatchReason.SAME_VAT_ID))));

        mvc.perform(post("/api/v1/business-partners").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.duplicates[0].reason").value("SAME_VAT_ID"));
    }

    @Test
    void returns404ProblemForUnknownId() throws Exception {
        when(findPartners.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/business-partners/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Business partner not found"));
    }

    @Test
    void returns400ForMalformedId() throws Exception {
        mvc.perform(get("/api/v1/business-partners/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
