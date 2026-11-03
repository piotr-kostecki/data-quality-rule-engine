package com.example.dq.cel;

import com.example.dq.engine.DataRecord;
import com.example.dq.engine.MissingFieldException;
import com.example.dq.engine.RuleLogic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CelLogicCompilerTest {

    private final CelLogicCompiler compiler = CelLogicCompiler.create();

    @Test
    void evaluatesAnExpressionAgainstTheRecord() {
        RuleLogic logic = compiler.compile("record.country == 'ZZ' ? 'blocked' : 'ok'");

        assertThat(logic.evaluate(DataRecord.of(Map.of("country", "ZZ")))).isEqualTo("blocked");
        assertThat(logic.evaluate(DataRecord.of(Map.of("country", "DE")))).isEqualTo("ok");
    }

    @Test
    void numbersAndBooleansFromJsonWork() {
        RuleLogic logic = compiler.compile("record.employees > 10 && record.active ? 'large' : 'small'");

        assertThat(logic.evaluate(DataRecord.of(Map.of("employees", 250, "active", true)))).isEqualTo("large");
    }

    @Test
    void reportsTheFieldsAnExpressionReferences() {
        RuleLogic logic = compiler.compile(
                "present(record, 'iban') && has(record.country) && record['legalName'] != '' ? record.iban : 'none'");

        assertThat(logic.referencedFields()).containsExactly("country", "iban", "legalName");
    }

    @Test
    void presentMeansExistingNonBlankText() {
        RuleLogic logic = compiler.compile("present(record, 'vatId') ? 'yes' : 'no'");
        Map<String, Object> withNull = new HashMap<>();
        withNull.put("vatId", null);

        assertThat(logic.evaluate(DataRecord.of(Map.of("vatId", "DE1")))).isEqualTo("yes");
        assertThat(logic.evaluate(DataRecord.of(Map.of()))).isEqualTo("no");
        assertThat(logic.evaluate(DataRecord.of(withNull))).isEqualTo("no");
        assertThat(logic.evaluate(DataRecord.of(Map.of("vatId", "  ")))).isEqualTo("no");
        assertThat(logic.evaluate(DataRecord.of(Map.of("vatId", 123)))).isEqualTo("no");
    }

    @Test
    void unguardedReadOfAnAbsentFieldIsAMissingField() {
        RuleLogic logic = compiler.compile("record.vatId.startsWith('DE') ? 'ok' : 'bad'");

        assertThatThrownBy(() -> logic.evaluate(DataRecord.of(Map.of("country", "DE"))))
                .isInstanceOfSatisfying(MissingFieldException.class, e -> assertThat(e.field()).isEqualTo("vatId"));
    }

    @Test
    void wrongValueTypeIsAnEvaluationError() {
        RuleLogic logic = compiler.compile("record.iban.startsWith('DE') ? 'ok' : 'bad'");

        assertThatThrownBy(() -> logic.evaluate(DataRecord.of(Map.of("iban", 12345))))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("record.iban.startsWith");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "record.country ==",                  // syntax error
            "size(record.vatId)",                 // result is an int, not a string
            "record.country == 'ZZ'",             // result is a bool: the decision belongs in the mapping
            "unknownFunction(record) ? 'a' : 'b'",
            "other.country == 'ZZ' ? 'a' : 'b'"   // only the record variable exists
    })
    void rejectsInvalidExpressionsWhenCompiling(String expression) {
        assertThatThrownBy(() -> compiler.compile(expression))
                .isInstanceOfSatisfying(RuleCompilationException.class, e -> assertThat(e.expression()).isEqualTo(expression));
    }
}
