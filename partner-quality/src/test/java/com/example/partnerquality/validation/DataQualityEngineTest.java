package com.example.partnerquality.validation;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.testing.PartnerFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataQualityEngineTest {

    @Test
    void validDataHasNoFindings() {
        assertThat(DataQualityEngine.withBuiltInRules().validate(PartnerFixtures.acme()).violations()).isEmpty();
    }

    @Test
    void collectsFindingsOfAllRules() {
        PartnerData broken = new PartnerData("Broken Corp",
                new Address(null, "1234", "Berlin", "DE"), "DE136695977", "DE89370400440532013001");

        ValidationReport report = DataQualityEngine.withBuiltInRules().validate(broken);

        assertThat(report.errors()).extracting(RuleViolation::ruleId)
                .containsExactly(RuleIds.POSTAL_CODE_FORMAT, RuleIds.VAT_ID, RuleIds.IBAN);
        assertThat(report.warnings()).extracting(RuleViolation::field).containsExactly("address.street");
    }

    @Test
    void customRulesExtendTheBuiltInOnes() {
        DataQualityRule noShouting = new DataQualityRule() {
            @Override
            public String id() {
                return "NO_SHOUTING";
            }

            @Override
            public List<RuleViolation> check(PartnerData data) {
                return data.name() != null && data.name().equals(data.name().toUpperCase())
                        ? List.of(RuleViolation.warning(id(), "name", "Name is all upper case"))
                        : List.of();
            }
        };

        DataQualityEngine engine = DataQualityEngine.withBuiltInRules().with(noShouting);
        PartnerData shouting = new PartnerData("ACME", PartnerFixtures.berlin(), PartnerFixtures.VALID_DE_VAT, null);

        assertThat(engine.ruleIds()).endsWith("NO_SHOUTING");
        assertThat(engine.validate(shouting).warnings()).extracting(RuleViolation::ruleId).containsExactly("NO_SHOUTING");
    }

    @Test
    void crashingRuleIsReportedAsErrorWithoutHidingOtherFindings() {
        DataQualityRule broken = new DataQualityRule() {
            @Override
            public String id() {
                return "BROKEN";
            }

            @Override
            public List<RuleViolation> check(PartnerData data) {
                throw new IllegalStateException("boom");
            }
        };

        ValidationReport report = DataQualityEngine.of(List.of(broken, BuiltInRules.mandatoryFields()))
                .validate(new PartnerData(null, Address.empty(), null, null));

        assertThat(report.errors()).extracting(RuleViolation::ruleId)
                .contains(RuleIds.RULE_FAILURE, RuleIds.MANDATORY_FIELDS);
        assertThat(report.errors().getFirst().message()).contains("BROKEN", "boom");
    }
}
