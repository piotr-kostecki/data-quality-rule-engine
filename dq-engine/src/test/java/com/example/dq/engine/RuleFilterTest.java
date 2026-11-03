package com.example.dq.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleFilterTest {

    private final Rule worldTax = TestRules.presence("worldTax", "vatId").categories("tax").build();
    private final Rule germanBank = TestRules.presence("germanBank", "iban")
            .countryScope(CountryScope.of("DE")).categories("bank").status(RuleStatus.DRAFT).build();

    @Test
    void filtersByStatus() {
        assertThat(RuleFilter.status(RuleStatus.DRAFT).test(germanBank)).isTrue();
        assertThat(RuleFilter.status(RuleStatus.DRAFT).test(worldTax)).isFalse();
    }

    @Test
    void filtersByCategory() {
        assertThat(RuleFilter.category("tax", "other").test(worldTax)).isTrue();
        assertThat(RuleFilter.category("tax").test(germanBank)).isFalse();
    }

    @Test
    void countryFilterKeepsWorldRulesAndRulesForThatCountry() {
        assertThat(RuleFilter.country("de").test(germanBank)).isTrue();
        assertThat(RuleFilter.country("FR").test(germanBank)).isFalse();
        assertThat(RuleFilter.country("FR").test(worldTax)).isTrue();
    }

    @Test
    void filtersCompose() {
        RuleFilter taxOrDraft = RuleFilter.category("tax").or(RuleFilter.status(RuleStatus.DRAFT));
        RuleFilter bankButNotDraft = RuleFilter.category("bank").and(RuleFilter.status(RuleStatus.DRAFT).negate());

        assertThat(taxOrDraft.test(worldTax)).isTrue();
        assertThat(taxOrDraft.test(germanBank)).isTrue();
        assertThat(bankButNotDraft.test(germanBank)).isFalse();
    }
}
