package com.example.dq.cel;

/**
 * A compiled expression failed on one record, e.g. a type mismatch. The engine reports it as an evaluation error for
 * that rule and record; a missing field is reported as {@link com.example.dq.engine.MissingFieldException} instead.
 */
public class ExpressionEvaluationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExpressionEvaluationException(String expression, String problem, Throwable cause) {
        super("Expression '" + expression + "' failed: " + problem, cause);
    }
}
