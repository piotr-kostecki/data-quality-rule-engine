package com.example.partnerquality.validation;

import com.example.partnerquality.PartnerData;

import java.util.List;

/**
 * A single, independent data quality check. This is the library's extension point: implement it to add
 * company- or country-specific rules, then pass them to {@link DataQualityEngine#of(List)}.
 *
 * <p>Implementations must be thread-safe and side-effect free; the engine may call them concurrently.
 */
public interface DataQualityRule {

    /** Stable id, reported in every {@link RuleViolation} this rule produces. */
    String id();

    /** Returns all findings for {@code data}, or an empty list when the data satisfies the rule. */
    List<RuleViolation> check(PartnerData data);
}
