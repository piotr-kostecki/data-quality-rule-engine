package com.example.dq.engine;

/** What a rule decided for one record. Produced by a rule's {@link DecisionMapping}, never by its logic. */
public enum Decision {
    VALID,
    INVALID,
    REVIEW,
    NOT_APPLICABLE
}
