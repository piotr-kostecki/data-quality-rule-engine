package com.example.dq.cel;

import com.example.dq.engine.Decision;
import com.example.dq.engine.DecisionMapping;
import com.example.dq.engine.Rule;
import com.example.dq.engine.RuleCatalog;
import com.example.dq.engine.RuleStatus;
import com.example.dq.engine.Severity;

/** The three rules of the task's scenario (raw/REQUIREMENT.md §5), with their logic written in CEL. */
final class FixtureRules {

    static final CelLogicCompiler CEL = CelLogicCompiler.create();

    static final Rule COUNTRY_BLOCKED = Rule.builder("countryBlocked")
            .label("Country is not blocked")
            .status(RuleStatus.RELEASED)
            .severity(Severity.ERROR)
            .logic(CEL.compile("record.country == 'ZZ' ? 'blocked' : 'ok'"))
            .mapping(DecisionMapping.when("blocked", Decision.INVALID).otherwise(Decision.NOT_APPLICABLE))
            .build();

    static final Rule VAT_FORMAT = Rule.builder("vatFormat")
            .label("VAT ID has a plausible length")
            .status(RuleStatus.RELEASED)
            .severity(Severity.ERROR)
            .categories("tax")
            .logic(CEL.compile("present(record, 'vatId') && size(record.vatId) >= 9 ? 'ok' : 'bad'"))
            .mapping(DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault())
            .build();

    static final Rule IBAN_FORMAT = Rule.builder("ibanFormat")
            .label("IBAN matches the record's country")
            .status(RuleStatus.RELEASED)
            .severity(Severity.ERROR)
            .categories("banking")
            .logic(CEL.compile("present(record, 'iban') && record.iban.startsWith(record.country) ? 'ok' : 'bad'"))
            .mapping(DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault())
            .build();

    static final RuleCatalog CATALOG = RuleCatalog.of(COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT);

    private FixtureRules() {
    }
}
