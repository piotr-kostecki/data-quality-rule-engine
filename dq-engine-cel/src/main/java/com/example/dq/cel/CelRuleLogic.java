package com.example.dq.cel;

import com.example.dq.engine.DataRecord;
import com.example.dq.engine.MissingFieldException;
import com.example.dq.engine.RuleLogic;
import dev.cel.common.CelErrorCode;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A compiled CEL expression as {@link RuleLogic}. Immutable and thread-safe, like the CEL program it wraps. */
final class CelRuleLogic implements RuleLogic {

    /** CEL reports an absent map key as e.g. {@code key 'vatId' is not present in map}. */
    private static final Pattern MISSING_KEY = Pattern.compile("'([^']+)'");

    private final String expression;
    private final CelRuntime.Program program;
    private final Set<String> referencedFields;

    CelRuleLogic(String expression, CelRuntime.Program program, Set<String> referencedFields) {
        this.expression = expression;
        this.program = program;
        this.referencedFields = referencedFields;
    }

    @Override
    public String evaluate(DataRecord record) {
        try {
            return (String) program.eval(Map.of(CelLogicCompiler.RECORD, record.fields()));
        } catch (CelEvaluationException e) {
            if (e.getErrorCode() == CelErrorCode.ATTRIBUTE_NOT_FOUND) {
                throw new MissingFieldException(missingField(e, record));
            }
            throw new ExpressionEvaluationException(expression, e.getMessage(), e);
        }
    }

    @Override
    public Set<String> referencedFields() {
        return referencedFields;
    }

    /** The expression's source text. */
    @Override
    public String toString() {
        return expression;
    }

    private String missingField(CelEvaluationException e, DataRecord record) {
        Matcher matcher = MISSING_KEY.matcher(String.valueOf(e.getMessage()));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return referencedFields.stream().filter(field -> !record.has(field)).findFirst().orElse("?");
    }
}
