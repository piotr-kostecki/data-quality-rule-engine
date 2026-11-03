package com.example.dq.cel;

import com.example.dq.engine.DataRecord;
import com.example.dq.engine.Decision;
import com.example.dq.engine.DecisionMapping;
import com.example.dq.engine.FieldValue;
import com.example.dq.engine.RecordRef;
import com.example.dq.engine.Rule;
import com.example.dq.engine.RuleCatalog;
import com.example.dq.engine.RuleError;
import com.example.dq.engine.RuleStatus;
import com.example.dq.engine.ValidationEngine;
import com.example.dq.engine.ValidationReport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fault isolation demanded by raw/REQUIREMENT.md §5: one rule that fails on a record and one broken record. The rest
 * of the batch completes, and each failure is reported with enough detail to find its cause.
 */
class FaultIsolationTest {

    /** Reads vatId without a guard, so it fails on r3, which has no VAT ID. */
    private static final Rule VAT_PREFIX = Rule.builder("vatPrefix")
            .status(RuleStatus.RELEASED)
            .logic(FixtureRules.CEL.compile("record.vatId.startsWith(record.country) ? 'ok' : 'bad'"))
            .mapping(DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault())
            .build();

    @Test
    void failingRuleAndBrokenRecordDoNotStopTheBatch() {
        List<DataRecord> records = new ArrayList<>(FixtureRecords.load());
        records.add(DataRecord.of(Map.of("id", "r4", "country", 42, "vatId", "DE111111111")));
        ValidationEngine engine = ValidationEngine.builder()
                .catalog(RuleCatalog.of(FixtureRules.COUNTRY_BLOCKED, FixtureRules.VAT_FORMAT, FixtureRules.IBAN_FORMAT, VAT_PREFIX))
                .build();

        ValidationReport report = engine.validate(records);

        assertThat(report.errors()).hasSize(2);
        assertThat(report.errors().get(0)).satisfies(error -> {
            assertThat(error.record()).isEqualTo(new RecordRef("r3", 2));
            assertThat(error.ruleId()).isEqualTo("vatPrefix");
            assertThat(error.kind()).isEqualTo(RuleError.Kind.MISSING_FIELD);
            assertThat(error.message()).contains("vatId");
            assertThat(error.fields()).containsExactly(new FieldValue("country", "ZZ"), new FieldValue("vatId", null));
        });
        assertThat(report.errors().get(1)).satisfies(error -> {
            assertThat(error.record()).isEqualTo(new RecordRef("r4", 3));
            assertThat(error.ruleId()).isNull();
            assertThat(error.kind()).isEqualTo(RuleError.Kind.BAD_RECORD);
        });

        // r1-r3 still get all 3 fixture rules; vatPrefix still decides r1 (VALID) and r2 (VALID)
        assertThat(report.results()).hasSize(9 + 2);
        assertThat(report.result("r3", "ibanFormat").orElseThrow().decision()).isEqualTo(Decision.INVALID);
        assertThat(report.result("r2", "vatPrefix").orElseThrow().decision()).isEqualTo(Decision.VALID);
        assertThat(report.summary().records()).isEqualTo(4);
        assertThat(report.summary().errors()).isEqualTo(2);
    }
}
