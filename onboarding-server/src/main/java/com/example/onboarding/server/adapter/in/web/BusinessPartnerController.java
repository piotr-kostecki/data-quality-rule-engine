package com.example.onboarding.server.adapter.in.web;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.PartnerId;
import com.example.onboarding.application.port.in.CheckPartnerUseCase;
import com.example.onboarding.application.port.in.FindPartnersQuery;
import com.example.onboarding.application.port.in.OnboardPartnerCommand;
import com.example.onboarding.application.port.in.OnboardPartnerUseCase;
import com.example.onboarding.application.port.in.OnboardingResult.DuplicateSuspected;
import com.example.onboarding.application.port.in.OnboardingResult.Onboarded;
import com.example.onboarding.application.port.in.OnboardingResult.Rejected;
import com.example.onboarding.server.adapter.in.web.dto.CheckResponse;
import com.example.onboarding.server.adapter.in.web.dto.OnboardingResponse;
import com.example.onboarding.server.adapter.in.web.dto.PartnerRequest;
import com.example.onboarding.server.adapter.in.web.dto.PartnerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Driving adapter: translates HTTP into calls on the incoming ports and their results back into HTTP.
 * It depends on the use-case interfaces only, never on the service that implements them.
 */
@RestController
@RequestMapping("/api/v1/business-partners")
@Tag(name = "Business partners", description = "Onboard customers and suppliers with data quality and duplicate checks")
public class BusinessPartnerController {

    private final OnboardPartnerUseCase onboardPartner;
    private final CheckPartnerUseCase checkPartner;
    private final FindPartnersQuery findPartners;

    public BusinessPartnerController(
            OnboardPartnerUseCase onboardPartner, CheckPartnerUseCase checkPartner, FindPartnersQuery findPartners) {
        this.onboardPartner = onboardPartner;
        this.checkPartner = checkPartner;
        this.findPartners = findPartners;
    }

    @PostMapping("/checks")
    @Operation(summary = "Dry run: report rule violations and duplicate candidates without storing anything")
    public CheckResponse check(@RequestBody PartnerRequest request) {
        return CheckResponse.from(checkPartner.check(request.toPartnerData()));
    }

    @PostMapping
    @Operation(summary = "Onboard a business partner",
            description = "201 when created, 422 when a data quality rule fails, 409 when it duplicates an existing partner")
    public ResponseEntity<?> onboard(@RequestBody PartnerRequest request, UriComponentsBuilder uriBuilder) {
        return switch (onboardPartner.onboard(new OnboardPartnerCommand(request.toPartnerData()))) {
            case Onboarded onboarded -> {
                URI location = uriBuilder.path("/api/v1/business-partners/{id}")
                        .buildAndExpand(onboarded.partner().id().value()).toUri();
                yield ResponseEntity.created(location).body(OnboardingResponse.from(onboarded));
            }
            case Rejected rejected -> ResponseEntity.of(ApiProblems.validationFailed(rejected.report())).build();
            case DuplicateSuspected duplicate -> ResponseEntity.of(ApiProblems.duplicateSuspected(duplicate.candidates())).build();
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a business partner by id")
    public PartnerResponse get(@PathVariable UUID id) {
        PartnerId partnerId = new PartnerId(id);
        return findPartners.findById(partnerId)
                .map(PartnerResponse::from)
                .orElseThrow(() -> ApiProblems.notFound(partnerId));
    }

    @GetMapping
    @Operation(summary = "List business partners, optionally filtered by ISO country code")
    public List<PartnerResponse> list(@RequestParam(required = false) String country) {
        List<BusinessPartner> partners = country == null ? findPartners.findAll() : findPartners.findByCountry(country);
        return partners.stream().map(PartnerResponse::from).toList();
    }
}
