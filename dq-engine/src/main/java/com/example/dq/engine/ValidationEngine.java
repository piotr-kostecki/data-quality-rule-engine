package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the released, selected rules of a catalog over a stream of records.
 *
 * <ul>
 *   <li><b>Streaming:</b> records are pulled lazily in batches of {@link RunOptions#batchSize()}, and every outcome is
 *       pushed to the {@link ResultSink} as soon as it is produced. Memory depends on the batch size and the number of
 *       rules, never on the number of records.</li>
 *   <li><b>Fault isolation:</b> a rule that throws, or a record that can't be evaluated, becomes a {@link RuleError};
 *       the next rule and the next record still run.</li>
 *   <li><b>Applicability:</b> a rule runs on a record only if its country scope is world or includes the record's
 *       country.</li>
 * </ul>
 *
 * <p>Immutable and thread-safe: build it once and share it. The catalog is read once, when the engine is built.
 * Runs are sequential on the caller's thread; the engine starts no threads.
 */
public final class ValidationEngine {

    private final RuleCatalog catalog;
    private final RecordSchema schema;

    private ValidationEngine(RuleCatalog catalog, RecordSchema schema) {
        this.catalog = RuleCatalog.of(catalog.rules());
        this.schema = schema;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** All rules of the catalog, whatever their status, in catalog order. */
    public List<Rule> rules() {
        return catalog.rules();
    }

    /** Runs with {@link RunOptions#DEFAULT}. */
    public RunSummary run(Iterable<DataRecord> records, ResultSink sink) {
        return run(records, sink, RunOptions.DEFAULT);
    }

    /**
     * Evaluates every record and pushes each outcome to {@code sink}.
     *
     * @return the run's counts
     * @throws RunAbortedException if the record source or the sink throws; carries the counts so far
     */
    public RunSummary run(Iterable<DataRecord> records, ResultSink sink, RunOptions options) {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(sink, "sink");
        Objects.requireNonNull(options, "options");

        RulePlan plan = RulePlan.select(catalog, options.filter());
        SummaryCounter counter = new SummaryCounter(plan.size());
        Run run = new Run(plan, sink, options, counter);
        try {
            List<DataRecord> batch = new ArrayList<>(Math.min(options.batchSize(), 10_000));
            for (DataRecord record : records) {
                batch.add(Objects.requireNonNull(record, "record source returned null"));
                if (batch.size() == options.batchSize()) {
                    run.evaluate(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                run.evaluate(batch);
            }
        } catch (RuntimeException e) {
            throw new RunAbortedException(counter.snapshot(), e);
        }
        return counter.snapshot();
    }

    /** Collects everything with {@link RunOptions#DEFAULT}; for bounded input only. */
    public ValidationReport validate(List<DataRecord> records) {
        return validate(records, RunOptions.DEFAULT);
    }

    /** Collects all outcomes in memory; for bounded input only, e.g. one HTTP request. */
    public ValidationReport validate(List<DataRecord> records, RunOptions options) {
        List<Result> results = new ArrayList<>();
        List<RuleError> errors = new ArrayList<>();
        List<RuleSkipped> skipped = new ArrayList<>();
        RunSummary summary = run(records, new ResultSink() {
            @Override
            public void onResult(Result result) {
                results.add(result);
            }

            @Override
            public void onError(RuleError error) {
                errors.add(error);
            }

            @Override
            public void onSkipped(RuleSkipped skip) {
                skipped.add(skip);
            }
        }, options);
        return new ValidationReport(results, errors, skipped, summary);
    }

    /** State of one run. */
    private final class Run {

        private final RulePlan plan;
        private final ResultSink sink;
        private final RunOptions options;
        private final SummaryCounter counter;
        private long position;
        private long batchIndex;

        Run(RulePlan plan, ResultSink sink, RunOptions options, SummaryCounter counter) {
            this.plan = plan;
            this.sink = sink;
            this.options = options;
            this.counter = counter;
        }

        void evaluate(List<DataRecord> batch) {
            long first = position;
            for (DataRecord record : batch) {
                evaluate(record, new RecordRef(idOf(record), position++));
            }
            sink.onBatchEnd(new BatchInfo(batchIndex++, first, batch.size()));
        }

        private void evaluate(DataRecord record, RecordRef ref) {
            counter.record();
            Object country = record.get(schema.countryField());
            if (country != null && !(country instanceof String)) {
                emit(new RuleError(ref, null, RuleError.Kind.BAD_RECORD,
                        "Field '%s' must be text, got %s".formatted(schema.countryField(), country.getClass().getSimpleName()),
                        List.of(), null));
                return;
            }
            String normalizedCountry = country == null || ((String) country).isBlank() ? null : CountryScope.normalize((String) country);

            for (Rule rule : plan.applicableTo(normalizedCountry)) {
                emit(evaluate(rule, record, ref));
            }
            List<Rule> skipped = plan.skippedFor(normalizedCountry);
            counter.skipped(skipped.size());
            if (options.explainLevel() == ExplainLevel.SKIPS) {
                for (Rule rule : skipped) {
                    sink.onSkipped(new RuleSkipped(ref, rule.id(), RuleSkipped.Reason.OUT_OF_COUNTRY_SCOPE));
                }
            }
        }

        /** Never throws: whatever the rule does wrong becomes a {@link RuleError}. */
        private Outcome evaluate(Rule rule, DataRecord record, RecordRef ref) {
            List<FieldValue> fields = fieldsOf(rule, record);
            try {
                String value = rule.logic().evaluate(record);
                if (value == null) {
                    return new RuleError(ref, rule.id(), RuleError.Kind.EVALUATION_FAILED, "Rule logic returned no value", fields, null);
                }
                DecisionMapping mapping = rule.mapping();
                return new Result(mapping.decide(value), rule.severity(),
                        new Provenance(rule.id(), ref, value, fields, mapping.usesDefault(value)));
            } catch (MissingFieldException e) {
                return new RuleError(ref, rule.id(), RuleError.Kind.MISSING_FIELD, e.getMessage(), fields, e.getClass().getName());
            } catch (RuntimeException | StackOverflowError e) {
                return new RuleError(ref, rule.id(), RuleError.Kind.EVALUATION_FAILED,
                        "Rule " + rule.id() + " failed: " + e, fields, e.getClass().getName());
            }
        }

        private void emit(Outcome outcome) {
            switch (outcome) {
                case Result result -> {
                    counter.result(result);
                    sink.onResult(result);
                }
                case RuleError error -> {
                    counter.error();
                    sink.onError(error);
                }
                case RuleSkipped skipped -> sink.onSkipped(skipped);
            }
        }

        private @Nullable String idOf(DataRecord record) {
            Object id = record.get(schema.idField());
            return id == null ? null : id.toString();
        }

        private static List<FieldValue> fieldsOf(Rule rule, DataRecord record) {
            try {
                return rule.logic().referencedFields().stream()
                        .sorted()
                        .map(field -> new FieldValue(field, record.get(field)))
                        .toList();
            } catch (RuntimeException e) {
                return List.of();
            }
        }
    }

    /** Builder for {@link ValidationEngine}. */
    public static final class Builder {

        private @Nullable RuleCatalog catalog;
        private RecordSchema schema = RecordSchema.DEFAULT;

        private Builder() {
        }

        public Builder catalog(RuleCatalog catalog) {
            this.catalog = Objects.requireNonNull(catalog, "catalog");
            return this;
        }

        /** Which fields hold the record id and country; default {@link RecordSchema#DEFAULT}. */
        public Builder schema(RecordSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema");
            return this;
        }

        /** @throws IllegalArgumentException if the catalog contains duplicate rule ids */
        public ValidationEngine build() {
            return new ValidationEngine(Objects.requireNonNull(catalog, "catalog is required"), schema);
        }
    }
}
