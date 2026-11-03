package com.example.dq.engine;

/** Lifecycle of a rule. The engine runs {@link #RELEASED} rules only, whatever the caller's filter selects. */
public enum RuleStatus {
    DRAFT,
    RELEASED,
    DEPRECATED
}
