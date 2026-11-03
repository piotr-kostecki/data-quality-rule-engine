package com.example.dq.engine;

import java.util.Objects;

/**
 * Per-run settings.
 *
 * @param filter       which rules to run; {@code RELEASED} is always enforced on top
 * @param batchSize    records read, evaluated and emitted together; bounds the memory a run uses
 * @param explainLevel how much is reported besides results and errors
 */
public record RunOptions(RuleFilter filter, int batchSize, ExplainLevel explainLevel) {

    public static final int DEFAULT_BATCH_SIZE = 1_000;

    /** All released rules, batches of {@value #DEFAULT_BATCH_SIZE}, {@link ExplainLevel#RESULTS}. */
    public static final RunOptions DEFAULT = new RunOptions(RuleFilter.all(), DEFAULT_BATCH_SIZE, ExplainLevel.RESULTS);

    public RunOptions {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(explainLevel, "explainLevel");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1, got " + batchSize);
        }
    }

    public RunOptions withFilter(RuleFilter filter) {
        return new RunOptions(filter, batchSize, explainLevel);
    }

    public RunOptions withBatchSize(int batchSize) {
        return new RunOptions(filter, batchSize, explainLevel);
    }

    public RunOptions withExplainLevel(ExplainLevel explainLevel) {
        return new RunOptions(filter, batchSize, explainLevel);
    }
}
