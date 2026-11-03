package com.example.partnerquality.testing;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;

/**
 * Valid sample data for tests, published as the library's test fixtures so consumers do not have to find
 * IBANs and VAT IDs with correct check digits themselves.
 *
 * <p>Gradle: {@code testImplementation(testFixtures("com.example:partner-quality:<version>"))}
 */
public final class PartnerFixtures {

    public static final String VALID_DE_VAT = "DE136695976";
    public static final String VALID_DE_IBAN = "DE89370400440532013000";
    public static final String VALID_PL_NIP = "PL1234563218";
    public static final String VALID_PL_IBAN = "PL61109010140000071219812874";
    public static final String VALID_CH_UID = "CHE116281710";
    public static final String VALID_CH_IBAN = "CH9300762011623852957";

    private PartnerFixtures() {
    }

    public static Address berlin() {
        return new Address("Industriestraße 1", "10115", "Berlin", "DE");
    }

    /** A fully valid German partner. */
    public static PartnerData acme() {
        return new PartnerData("Acme Chemicals GmbH", berlin(), VALID_DE_VAT, VALID_DE_IBAN);
    }

    /** A fully valid Polish partner. */
    public static PartnerData polishLogistics() {
        return new PartnerData("Przykład Logistyka sp. z o.o.",
                new Address("ul. Testowa 1", "30-001", "Kraków", "PL"), VALID_PL_NIP, VALID_PL_IBAN);
    }

    /** A fully valid Swiss partner. */
    public static PartnerData alpen() {
        return new PartnerData("Alpen Präzision AG",
                new Address("Bahnhofstrasse 10", "8001", "Zürich", "CH"), VALID_CH_UID, VALID_CH_IBAN);
    }
}
