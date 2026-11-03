package com.example.dq.cel;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.ast.CelConstant;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.ast.CelExpr.ExprKind.Kind;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.navigation.CelNavigableExpr;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Finds the record fields an expression references, from its checked syntax tree: {@code record.x},
 * {@code has(record.x)}, {@code record["x"]} and {@code present(record, "x")}. These are reported as provenance.
 */
final class ReferencedFields {

    private static final String INDEX = "_[_]";

    private ReferencedFields() {
    }

    static Set<String> of(CelAbstractSyntaxTree ast) {
        Set<String> fields = new TreeSet<>();
        CelNavigableAst.fromAst(ast).getRoot().allNodes()
                .forEach((CelNavigableExpr node) -> fieldOf(node.expr()).ifPresent(fields::add));
        return Collections.unmodifiableSet(fields);
    }

    private static Optional<String> fieldOf(CelExpr expr) {
        if (expr.getKind() == Kind.SELECT && isRecord(expr.select().operand())) {
            return Optional.of(expr.select().field());
        }
        if (expr.getKind() == Kind.CALL) {
            String function = expr.call().function();
            List<CelExpr> args = expr.call().args();
            if ((function.equals(RecordFunctions.PRESENT) || function.equals(INDEX))
                    && args.size() == 2 && isRecord(args.get(0)) && isStringConstant(args.get(1))) {
                return Optional.of(args.get(1).constant().stringValue());
            }
        }
        return Optional.empty();
    }

    private static boolean isRecord(CelExpr expr) {
        return expr.getKind() == Kind.IDENT && expr.ident().name().equals(CelLogicCompiler.RECORD);
    }

    private static boolean isStringConstant(CelExpr expr) {
        return expr.getKind() == Kind.CONSTANT && expr.constant().getKind() == CelConstant.Kind.STRING_VALUE;
    }
}
