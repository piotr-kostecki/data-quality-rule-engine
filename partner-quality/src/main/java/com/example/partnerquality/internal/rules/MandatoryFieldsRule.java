package com.example.partnerquality.internal.rules;

import com.example.partnerquality.PartnerData;
import com.example.partnerquality.validation.DataQualityRule;
import com.example.partnerquality.validation.RuleIds;
import com.example.partnerquality.validation.RuleViolation;

import java.util.ArrayList;
import java.util.List;

/** Name, country and city identify a company; street and postal code are wanted but not required. */
public final class MandatoryFieldsRule implements DataQualityRule {

    private static final String ID = RuleIds.MANDATORY_FIELDS;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<RuleViolation> check(PartnerData data) {
        List<RuleViolation> violations = new ArrayList<>();
        if (data.name() == null) {
            violations.add(RuleViolation.error(ID, "name", "Name is required"));
        }
        if (data.address().country() == null) {
            violations.add(RuleViolation.error(ID, "address.country", "Country is required"));
        }
        if (data.address().city() == null) {
            violations.add(RuleViolation.error(ID, "address.city", "City is required"));
        }
        if (data.address().street() == null) {
            violations.add(RuleViolation.warning(ID, "address.street", "Street is missing"));
        }
        if (data.address().postalCode() == null) {
            violations.add(RuleViolation.warning(ID, "address.postalCode", "Postal code is missing"));
        }
        return violations;
    }
}
