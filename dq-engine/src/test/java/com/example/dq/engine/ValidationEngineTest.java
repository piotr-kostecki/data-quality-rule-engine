package com.example.dq.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.dq.engine.TestRules.presence;
import static com.example.dq.engine.TestRules.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationEngineTest {

    private static ValidationEngine engine(Rule... rules) {
        return ValidationEngine.builder().catalog(RuleCatalog.of(rules)).build();
    }

    @Test
    void producesOneExplainedResultPerRuleAndRecord() {
        ValidationEngine engine = engine(presence("vatPresent", "vatId").severity(Severity.WARNING).build());

        ValidationReport report = engine.validate(List.of(record("id", "r1", "vatId", "DE1"), record("id", "r2")));

        assertThat(report.results()).hasSize(2);
        Result missing = report.result("r2", "vatPresent").orElseThrow();
        assertThat(missing.decision()).isEqualTo(Decision.INVALID);
        assertThat(missing.severity()).isEqualTo(Severity.WARNING);
        assertThat(missing.provenance().value()).isEqualTo("bad");
        assertThat(missing.provenance().record()).isEqualTo(new RecordRef("r2", 1));
        assertThat(missing.provenance().fields()).containsExactly(new FieldValue("vatId", null));
        assertThat(missing.provenance().defaultMapped()).isFalse();
    }

    @Test
    void runsReleasedRulesOnlyWhateverTheFilterSelects() {
        ValidationEngine engine = engine(
                presence("released", "a").build(),
                presence("draft", "a").status(RuleStatus.DRAFT).build(),
                presence("deprecated", "a").status(RuleStatus.DEPRECATED).build());

        ValidationReport all = engine.validate(List.of(record("a", "x")));
        ValidationReport draftsOnly = engine.validate(List.of(record("a", "x")),
                RunOptions.DEFAULT.withFilter(RuleFilter.status(RuleStatus.DRAFT)));

        assertThat(all.results()).extracting(Result::ruleId).containsExactly("released");
        assertThat(draftsOnly.results()).isEmpty();
        assertThat(draftsOnly.summary().rulesSelected()).isZero();
    }

    @Test
    void filterNarrowsTheRun() {
        ValidationEngine engine = engine(presence("tax", "a").categories("tax").build(), presence("bank", "a").categories("bank").build());

        ValidationReport report = engine.validate(List.of(record("a", "x")), RunOptions.DEFAULT.withFilter(RuleFilter.category("bank")));

        assertThat(report.results()).extracting(Result::ruleId).containsExactly("bank");
    }

    @Test
    void countryScopedRuleRunsOnlyOnRecordsFromThatCountry() {
        ValidationEngine engine = engine(
                presence("world", "a").build(),
                presence("germanOnly", "a").countryScope(CountryScope.of("DE")).build());

        ValidationReport report = engine.validate(List.of(record("id", "de", "country", "de", "a", "x"), record("id", "fr", "country", "FR", "a", "x")));

        assertThat(report.results()).extracting(r -> r.record().id() + ":" + r.ruleId())
                .containsExactly("de:world", "de:germanOnly", "fr:world");
        assertThat(report.summary().skipped()).isEqualTo(1);
        assertThat(report.skipped()).as("skips are only counted at the default explain level").isEmpty();
    }

    @Test
    void A2_skipsAreEventsAtSkipsLevelAndNeverResults() {
        ValidationEngine engine = engine(presence("germanOnly", "a").countryScope(CountryScope.of("DE")).build());

        ValidationReport report = engine.validate(List.of(record("id", "fr", "country", "FR")),
                RunOptions.DEFAULT.withExplainLevel(ExplainLevel.SKIPS));

        assertThat(report.results()).isEmpty();
        assertThat(report.skipped()).containsExactly(new RuleSkipped(new RecordRef("fr", 0), "germanOnly", RuleSkipped.Reason.OUT_OF_COUNTRY_SCOPE));
        assertThat(report.summary().count(Decision.NOT_APPLICABLE)).isZero();
    }

    @Test
    void A3_recordWithoutOrWithUnknownCountryGetsWorldRulesOnly() {
        ValidationEngine engine = engine(presence("world", "a").build(), presence("germanOnly", "a").countryScope(CountryScope.of("DE")).build());

        ValidationReport report = engine.validate(List.of(record("id", "none"), record("id", "zz", "country", "ZZ"), record("id", "blank", "country", " ")));

        assertThat(report.results()).extracting(Result::ruleId).containsOnly("world").hasSize(3);
    }

    @Test
    void failingRuleBecomesAnErrorAndTheRunContinues() {
        Rule exploding = Rule.builder("exploding").status(RuleStatus.RELEASED)
                .logic(RuleLogic.of(Set.of("a"), r -> {
                    if ("boom".equals(r.get("a"))) {
                        throw new IllegalStateException("bad logic");
                    }
                    return "ok";
                }))
                .mapping(DecisionMapping.when("ok", Decision.VALID).withImplicitDefault())
                .build();
        ValidationEngine engine = engine(exploding, presence("other", "a").build());

        ValidationReport report = engine.validate(List.of(record("id", "r1", "a", "boom"), record("id", "r2", "a", "fine")));

        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.record()).isEqualTo(new RecordRef("r1", 0));
            assertThat(error.ruleId()).isEqualTo("exploding");
            assertThat(error.kind()).isEqualTo(RuleError.Kind.EVALUATION_FAILED);
            assertThat(error.exceptionType()).isEqualTo(IllegalStateException.class.getName());
            assertThat(error.message()).contains("bad logic");
            assertThat(error.fields()).containsExactly(new FieldValue("a", "boom"));
        });
        assertThat(report.results()).extracting(r -> r.record().id() + ":" + r.ruleId())
                .containsExactly("r1:other", "r2:exploding", "r2:other");
        assertThat(report.summary().errors()).isEqualTo(1);
        assertThat(report.summary().results()).isEqualTo(3);
    }

    @Test
    void missingRequiredFieldIsReportedWithTheFieldName() {
        Rule strict = Rule.builder("strict").status(RuleStatus.RELEASED)
                .logic(RuleLogic.of(Set.of("legalForm"), r -> {
                    if (!r.has("legalForm")) {
                        throw new MissingFieldException("legalForm");
                    }
                    return "ok";
                }))
                .mapping(DecisionMapping.when("ok", Decision.VALID).withImplicitDefault())
                .build();

        ValidationReport report = engine(strict).validate(List.of(record("id", "r1")));

        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.kind()).isEqualTo(RuleError.Kind.MISSING_FIELD);
            assertThat(error.message()).contains("legalForm");
        });
    }

    @Test
    void logicReturningNullIsAnError() {
        Rule broken = Rule.builder("broken").status(RuleStatus.RELEASED)
                .logic(RuleLogic.of(Set.of(), r -> null))
                .mapping(DecisionMapping.when("ok", Decision.VALID).withImplicitDefault())
                .build();

        assertThat(engine(broken).validate(List.of(record("id", "r1"))).errors())
                .singleElement().extracting(RuleError::kind).isEqualTo(RuleError.Kind.EVALUATION_FAILED);
    }

    @Test
    void recordWithANonTextCountryIsABadRecordAndTheNextRecordStillRuns() {
        ValidationReport report = engine(presence("rule", "a").build())
                .validate(List.of(record("id", "bad", "country", 42), record("id", "good", "country", "DE")));

        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.kind()).isEqualTo(RuleError.Kind.BAD_RECORD);
            assertThat(error.ruleId()).isNull();
            assertThat(error.message()).contains("country");
        });
        assertThat(report.results()).extracting(r -> r.record().id()).containsExactly("good");
        assertThat(report.summary().records()).isEqualTo(2);
    }

    @Test
    void unmappedValueUsesTheDefaultAndSaysSo() {
        Rule rule = Rule.builder("r").status(RuleStatus.RELEASED)
                .logic(RuleLogic.of(Set.of(), x -> "surprise"))
                .mapping(DecisionMapping.when("ok", Decision.VALID).withImplicitDefault())
                .build();

        Result result = engine(rule).validate(List.of(record())).results().getFirst();

        assertThat(result.decision()).isEqualTo(Decision.REVIEW);
        assertThat(result.provenance().defaultMapped()).isTrue();
    }

    @Test
    void summaryCountsByDecisionAndSeverity() {
        ValidationEngine engine = engine(presence("err", "a").build(), presence("info", "b").severity(Severity.INFO).build());

        RunSummary summary = engine.validate(List.of(record("a", "x"), record("b", "y"))).summary();

        assertThat(summary.records()).isEqualTo(2);
        assertThat(summary.results()).isEqualTo(4);
        assertThat(summary.count(Decision.VALID)).isEqualTo(2);
        assertThat(summary.count(Decision.INVALID)).isEqualTo(2);
        assertThat(summary.count(Decision.REVIEW)).isZero();
        assertThat(summary.count(Severity.ERROR)).isEqualTo(2);
        assertThat(summary.count(Severity.INFO)).isEqualTo(2);
        assertThat(summary.count(Severity.WARNING)).isZero();
    }

    @Test
    void emitsEachBatchBeforeReadingTheNext() {
        List<String> events = new ArrayList<>();
        ResultSink sink = new ResultSink() {
            @Override
            public void onResult(Result result) {
                events.add(result.record().id());
            }

            @Override
            public void onError(RuleError error) {
                events.add("error");
            }

            @Override
            public void onBatchEnd(BatchInfo batch) {
                events.add("batch" + batch.index() + "@" + batch.firstPosition() + "x" + batch.size());
            }
        };
        Iterable<DataRecord> source = () -> List.of("1", "2", "3", "4", "5").stream()
                .peek(id -> events.add("read" + id))
                .map(id -> record("id", id, "a", "x"))
                .iterator();

        engine(presence("rule", "a").build()).run(source, sink, RunOptions.DEFAULT.withBatchSize(2));

        assertThat(events).containsExactly(
                "read1", "read2", "1", "2", "batch0@0x2",
                "read3", "read4", "3", "4", "batch1@2x2",
                "read5", "5", "batch2@4x1");
    }

    @Test
    void failingSinkAbortsTheRunWithTheCountsSoFar() {
        ResultSink failing = new ResultSink() {
            @Override
            public void onResult(Result result) {
                if ("r2".equals(result.record().id())) {
                    throw new IllegalStateException("disk full");
                }
            }

            @Override
            public void onError(RuleError error) {
            }
        };

        assertThatThrownBy(() -> engine(presence("rule", "a").build())
                .run(List.of(record("id", "r1"), record("id", "r2"), record("id", "r3")), failing))
                .isInstanceOfSatisfying(RunAbortedException.class, e -> assertThat(e.partialSummary().records()).isEqualTo(2))
                .hasRootCauseMessage("disk full");
    }

    @Test
    void rejectsDuplicateRuleIds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine(presence("same", "a").build(), presence("same", "b").build()))
                .withMessageContaining("same");
    }

    @Test
    void rejectsAnInvalidBatchSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> RunOptions.DEFAULT.withBatchSize(0));
    }
}
