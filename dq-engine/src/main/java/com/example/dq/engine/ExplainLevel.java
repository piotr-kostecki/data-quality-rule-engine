package com.example.dq.engine;

/** How much a run reports beyond results and errors. Set by the operator, like a log level. */
public enum ExplainLevel {
    /** Results, errors and the summary; skipped rules are only counted. The default, for large runs. */
    RESULTS,
    /** Also one {@link RuleSkipped} event per selected rule that did not apply to a record. */
    SKIPS
}
