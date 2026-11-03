package com.example.dq.cel;

import com.example.dq.engine.RuleLogic;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;

/**
 * Compiles CEL expressions into {@link RuleLogic}.
 *
 * <p>An expression sees one variable, {@code record}: the record's fields as a map from field name to value. It
 * must evaluate to a {@code string}, the rule's computed value; the rule's decision mapping turns it into a
 * decision. Example: {@code present(record, "vatId") && size(record.vatId) >= 9 ? "ok" : "bad"}.
 *
 * <ul>
 *   <li>Reading an absent field ({@code record.x}) fails with a missing-field error for that record. Guard optional
 *       fields with {@code has(record.x)} or {@code present(record, "x")}.</li>
 *   <li>{@code present(record, "x")} is true when the field exists, is a string and is not blank.</li>
 *   <li>Everything else is the CEL standard library: comparisons, {@code size}, {@code startsWith},
 *       {@code matches}, {@code &&} / {@code ||}, the conditional operator, macros such as {@code has}.</li>
 * </ul>
 *
 * <p>Compilation type-checks the expression, so errors are found when rules are loaded, not while records are
 * validated. CEL has no side effects and always terminates. Immutable and thread-safe: create once and share.
 */
public final class CelLogicCompiler {

    /** Name of the variable that holds the record. */
    public static final String RECORD = "record";

    static final MapType RECORD_TYPE = RecordFunctions.recordType();

    private final CelCompiler compiler;
    private final CelRuntime runtime;

    private CelLogicCompiler() {
        this.compiler = CelCompilerFactory.standardCelCompilerBuilder()
                // has(), all(), exists(), exists_one(), map(), filter(): not enabled by default in CEL-Java
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .addVar(RECORD, RECORD_TYPE)
                .addFunctionDeclarations(RecordFunctions.declarations())
                .setResultType(SimpleType.STRING)
                .build();
        this.runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .addFunctionBindings(RecordFunctions.bindings())
                .build();
    }

    /** Compiler with the CEL standard library and the record helper functions. */
    public static CelLogicCompiler create() {
        return new CelLogicCompiler();
    }

    /**
     * Parses, type-checks and plans {@code expression}.
     *
     * @throws RuleCompilationException if the expression is invalid or does not evaluate to a string
     */
    public RuleLogic compile(String expression) {
        CelValidationResult result = compiler.compile(expression);
        if (result.hasError()) {
            throw new RuleCompilationException(expression, result.getErrorString());
        }
        try {
            CelAbstractSyntaxTree ast = result.getAst();
            return new CelRuleLogic(expression, runtime.createProgram(ast), ReferencedFields.of(ast));
        } catch (CelValidationException | CelEvaluationException e) {
            throw new RuleCompilationException(expression, String.valueOf(e.getMessage()));
        }
    }
}
