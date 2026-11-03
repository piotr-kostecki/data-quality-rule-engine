package com.example.partnerquality.matching;

/** Why a candidate was considered a match. New constants may be added in minor releases. */
public enum MatchReason {
    /** Same tax ID: practically certain to be the same legal entity. */
    SAME_VAT_ID,
    /** Same bank account: often the same company, but group companies may share a payment account. */
    SAME_IBAN,
    /** Similar normalized name, weighted by how well the location matches. */
    SIMILAR_NAME_AND_LOCATION
}
