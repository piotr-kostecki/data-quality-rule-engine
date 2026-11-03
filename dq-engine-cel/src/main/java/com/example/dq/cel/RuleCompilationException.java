package com.example.dq.cel;

/** An expression that does not compile: syntax error, unknown function, or a result that is not a string. */
public class RuleCompilationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String expression;

    public RuleCompilationException(String expression, String problem) {
        super("Invalid rule expression '" + expression + "': " + problem);
        this.expression = expression;
    }

    public String expression() {
        return expression;
    }
}
