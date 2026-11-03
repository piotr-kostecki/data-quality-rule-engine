package com.example.onboarding.server.adapter.in.web;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.PartnerId;
import com.example.onboarding.server.adapter.in.web.dto.DuplicateCandidateDto;
import com.example.onboarding.server.adapter.in.web.dto.ViolationDto;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.validation.ValidationReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.List;

/** RFC 9457 "Problem Details" bodies for every error this API returns. */
final class ApiProblems {

    private static final String TYPE_PREFIX = "urn:problem:partner-onboarding:";

    private ApiProblems() {
    }

    static ProblemDetail validationFailed(ValidationReport report) {
        ProblemDetail problem = create(HttpStatus.UNPROCESSABLE_ENTITY, "validation-failed", "Data quality check failed",
                "%d data quality rule(s) failed".formatted(report.errors().size()));
        problem.setProperty("violations", ViolationDto.fromAll(report.violations()));
        return problem;
    }

    static ProblemDetail duplicateSuspected(List<Match<BusinessPartner>> candidates) {
        ProblemDetail problem = create(HttpStatus.CONFLICT, "duplicate-suspected", "Business partner probably exists",
                "The partner matches an existing record; use it instead of creating a duplicate");
        problem.setProperty("duplicates", DuplicateCandidateDto.fromAll(candidates));
        return problem;
    }

    static ErrorResponseException notFound(PartnerId id) {
        ProblemDetail problem = create(HttpStatus.NOT_FOUND, "not-found", "Business partner not found",
                "No business partner with id " + id);
        return new ErrorResponseException(HttpStatus.NOT_FOUND, problem, null);
    }

    private static ProblemDetail create(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + type));
        problem.setTitle(title);
        return problem;
    }
}
