package com.example.dq.engine;

/** Thrown by {@link RuleLogic} when a field it requires is absent; reported as {@link RuleError.Kind#MISSING_FIELD}. */
public class MissingFieldException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public MissingFieldException(String field) {
        super("Required field '" + field + "' is missing");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
