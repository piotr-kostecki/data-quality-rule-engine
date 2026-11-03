package com.example.partnerquality.matching;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.testing.PartnerFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateDetectorTest {

    /** Stand-in for a consumer's own entity type: the detector must return it unchanged. */
    record StoredPartner(String id, PartnerData data) {
    }

    private final DuplicateDetector detector = DuplicateDetector.withDefaultPolicy();
    private final StoredPartner acme = new StoredPartner("acme-1", PartnerFixtures.acme());

    @Test
    void returnsTheCallersOwnObjects() {
        List<Match<StoredPartner>> matches = detector.findMatches(PartnerFixtures.acme(), List.of(acme), StoredPartner::data);

        assertThat(matches).singleElement().extracting(Match::candidate).isSameAs(acme);
    }

    @Test
    void sameVatIdIsACertainMatchEvenWithADifferentName() {
        PartnerData probe = new PartnerData("Totally Different Name", PartnerFixtures.berlin(), PartnerFixtures.VALID_DE_VAT, null);

        assertThat(find(probe))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.reason()).isEqualTo(MatchReason.SAME_VAT_ID);
                    assertThat(detector.isBlocking(m)).isTrue();
                });
    }

    @Test
    void sameIbanIsReportedButDoesNotBlock() {
        PartnerData probe = new PartnerData("Acme Group Treasury", new Address(null, null, "Frankfurt", "DE"), null, PartnerFixtures.VALID_DE_IBAN);

        assertThat(find(probe))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.reason()).isEqualTo(MatchReason.SAME_IBAN);
                    assertThat(detector.isBlocking(m)).isFalse();
                });
    }

    @Test
    void spellingVariantAtSameAddressBlocks() {
        PartnerData probe = new PartnerData("ACME Chemicals G.m.b.H.", PartnerFixtures.berlin(), null, null);

        assertThat(find(probe))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.reason()).isEqualTo(MatchReason.SIMILAR_NAME_AND_LOCATION);
                    assertThat(detector.isBlocking(m)).isTrue();
                });
    }

    @Test
    void sameNameInAnotherCityIsOnlyAPossibleDuplicate() {
        PartnerData probe = new PartnerData("Acme Chemicals GmbH", new Address(null, "80331", "München", "DE"), null, null);

        assertThat(find(probe))
                .singleElement()
                .satisfies(m -> assertThat(detector.isBlocking(m)).isFalse());
    }

    @Test
    void unrelatedCompanyIsNotACandidate() {
        PartnerData probe = new PartnerData("Zebra Software AG", PartnerFixtures.berlin(), null, null);

        assertThat(find(probe)).isEmpty();
    }

    @Test
    void locationAgreementScale() {
        Address base = new Address(null, "10115", "Berlin", "DE");
        assertThat(DuplicateDetector.locationAgreement(base, new Address(null, "10115", "BERLIN", "DE"))).isEqualTo(1.0);
        assertThat(DuplicateDetector.locationAgreement(base, new Address(null, "10117", "Berlin", "DE"))).isEqualTo(0.7);
        assertThat(DuplicateDetector.locationAgreement(base, new Address(null, null, null, "DE"))).isEqualTo(0.5);
        assertThat(DuplicateDetector.locationAgreement(base, new Address(null, "80331", "München", "DE"))).isEqualTo(0.0);
    }

    private List<Match<PartnerData>> find(PartnerData probe) {
        return detector.findMatches(probe, List.of(acme.data()));
    }
}
