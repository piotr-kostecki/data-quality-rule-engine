package com.example.dq.engine;

/**
 * Receives outcomes as they are produced, so a run never holds all results in memory. Called on the thread that
 * runs the engine, in input order. A sink that throws aborts the run with {@link RunAbortedException}: unlike a
 * rule, the sink is the host's own code.
 */
public interface ResultSink {

    void onResult(Result result);

    void onError(RuleError error);

    /** A rule that did not apply to a record; called only at {@link ExplainLevel#SKIPS}. */
    default void onSkipped(RuleSkipped skipped) {
    }

    /** A batch is complete: a place to flush, commit or checkpoint. */
    default void onBatchEnd(BatchInfo batch) {
    }
}
