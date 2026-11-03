package com.example.dq.engine;

/**
 * A run stopped because host code failed: the {@link ResultSink} or the record source threw. Rule failures never
 * cause this; they are reported as {@link RuleError}s.
 */
public class RunAbortedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient RunSummary partialSummary;

    public RunAbortedException(RunSummary partialSummary, Throwable cause) {
        super("Run aborted after " + partialSummary.records() + " records: " + cause, cause);
        this.partialSummary = partialSummary;
    }

    /** Counts up to the failure. */
    public RunSummary partialSummary() {
        return partialSummary;
    }
}
