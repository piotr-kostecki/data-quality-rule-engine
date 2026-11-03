package com.example.dq.cel;

import com.example.dq.engine.Decision;
import com.example.dq.engine.FieldValue;
import com.example.dq.engine.Provenance;
import com.example.dq.engine.Result;
import com.example.dq.engine.RunSummary;
import com.example.dq.engine.Severity;
import com.example.dq.engine.ValidationEngine;
import com.example.dq.engine.ValidationReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** The scenario of raw/REQUIREMENT.md §5: three records, three CEL rules, the expected decision table. */
class RequirementScenarioTest {

    private static ValidationReport report;

    @BeforeAll
    static void run() {
        report = ValidationEngine.builder().catalog(FixtureRules.CATALOG).build().validate(FixtureRecords.load());
    }

    @ParameterizedTest(name = "{0} / {1} -> {2}")
    @CsvSource({
            "r1, countryBlocked, NOT_APPLICABLE",
            "r1, vatFormat,      VALID",
            "r1, ibanFormat,     VALID",
            "r2, countryBlocked, NOT_APPLICABLE",
            "r2, vatFormat,      INVALID",
            "r2, ibanFormat,     VALID",
            "r3, countryBlocked, INVALID",
            "r3, vatFormat,      INVALID",
            "r3, ibanFormat,     INVALID"
    })
    void matchesTheExpectedDecisionTable(String recordId, String ruleId, Decision expected) {
        Result result = report.result(recordId, ruleId).orElseThrow();

        assertThat(result.decision()).isEqualTo(expected);
        assertThat(result.severity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void provenanceExplainsEachDecision() {
        Provenance shortVat = report.result("r2", "vatFormat").orElseThrow().provenance();
        assertThat(shortVat.ruleId()).isEqualTo("vatFormat");
        assertThat(shortVat.record().id()).isEqualTo("r2");
        assertThat(shortVat.value()).isEqualTo("bad");
        assertThat(shortVat.fields()).containsExactly(new FieldValue("vatId", "FR22"));

        Provenance noVat = report.result("r3", "vatFormat").orElseThrow().provenance();
        assertThat(noVat.fields()).containsExactly(new FieldValue("vatId", null));

        Provenance emptyIban = report.result("r3", "ibanFormat").orElseThrow().provenance();
        assertThat(emptyIban.fields()).containsExactly(new FieldValue("country", "ZZ"), new FieldValue("iban", ""));

        Provenance notBlocked = report.result("r1", "countryBlocked").orElseThrow().provenance();
        assertThat(notBlocked.value()).isEqualTo("ok");
        assertThat(notBlocked.defaultMapped()).as("'ok' falls through to the default NOT_APPLICABLE").isTrue();
    }

    @Test
    void summaryCountsResultsByDecisionAndSeverity() {
        RunSummary summary = report.summary();

        assertThat(summary.records()).isEqualTo(3);
        assertThat(summary.results()).isEqualTo(9);
        assertThat(summary.errors()).isZero();
        assertThat(summary.count(Decision.VALID)).isEqualTo(3);
        assertThat(summary.count(Decision.INVALID)).isEqualTo(4);
        assertThat(summary.count(Decision.NOT_APPLICABLE)).isEqualTo(2);
        assertThat(summary.count(Decision.REVIEW)).isZero();
        assertThat(summary.count(Severity.ERROR)).isEqualTo(9);
    }
}
