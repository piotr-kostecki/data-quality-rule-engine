package com.example.dq.cel;

import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelFunctionBinding;

import java.util.Map;

/** Helper functions available in every rule expression, in addition to the CEL standard library. */
final class RecordFunctions {

    /** {@code present(record, "field")}: the field exists, is a string and is not blank. */
    static final String PRESENT = "present";

    private static final String PRESENT_OVERLOAD = "present_map_string";

    private RecordFunctions() {
    }

    static CelFunctionDecl[] declarations() {
        return new CelFunctionDecl[] {
                CelFunctionDecl.newFunctionDeclaration(PRESENT, CelOverloadDecl.newGlobalOverload(
                        PRESENT_OVERLOAD, SimpleType.BOOL, CelLogicCompiler.RECORD_TYPE, SimpleType.STRING))
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static CelFunctionBinding[] bindings() {
        Class<Map<?, ?>> mapClass = (Class<Map<?, ?>>) (Class) Map.class;
        return new CelFunctionBinding[] {
                CelFunctionBinding.from(PRESENT_OVERLOAD, mapClass, String.class, RecordFunctions::present)
        };
    }

    static boolean present(Map<?, ?> record, String field) {
        return record.get(field) instanceof String value && !value.isBlank();
    }

    /** The CEL type of the {@code record} variable: a map from field name to any value. */
    static MapType recordType() {
        return MapType.create(SimpleType.STRING, SimpleType.DYN);
    }
}
