package com.example.partnerquality.internal.rules;

import com.example.partnerquality.PartnerData;
import com.example.partnerquality.internal.text.IsoCountries;
import com.example.partnerquality.validation.DataQualityRule;
import com.example.partnerquality.validation.RuleIds;
import com.example.partnerquality.validation.RuleViolation;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validates the VAT / tax ID in three steps: prefix matches the country, format matches the prefix,
 * check digit is correct (where we know the algorithm). A missing VAT ID is only a warning, because
 * it weakens duplicate detection but does not make the record wrong.
 */
public final class VatIdRule implements DataQualityRule {

    private static final String ID = RuleIds.VAT_ID;
    private static final String FIELD = "vatId";

    private static final Map<String, Pattern> FORMATS = Map.ofEntries(
            Map.entry("AT", Pattern.compile("ATU\\d{8}")),
            Map.entry("BE", Pattern.compile("BE[01]\\d{9}")),
            Map.entry("CH", Pattern.compile("CHE\\d{9}(MWST|TVA|IVA)?")),
            Map.entry("DE", Pattern.compile("DE\\d{9}")),
            Map.entry("EL", Pattern.compile("EL\\d{9}")),
            Map.entry("ES", Pattern.compile("ES[0-9A-Z]\\d{7}[0-9A-Z]")),
            Map.entry("FR", Pattern.compile("FR[0-9A-Z]{2}\\d{9}")),
            Map.entry("GB", Pattern.compile("GB(\\d{9}|\\d{12}|GD\\d{3}|HA\\d{3})")),
            Map.entry("IT", Pattern.compile("IT\\d{11}")),
            Map.entry("NL", Pattern.compile("NL\\d{9}B\\d{2}")),
            Map.entry("PL", Pattern.compile("PL\\d{10}")));

    private static final Map<String, Predicate<String>> CHECKSUMS = Map.of(
            "CH", VatIdRule::isValidSwissUid,
            "DE", VatIdRule::isValidGermanVatId,
            "PL", VatIdRule::isValidPolishNip);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<RuleViolation> check(PartnerData data) {
        String vatId = data.vatId();
        if (vatId == null) {
            return List.of(RuleViolation.warning(ID, FIELD, "VAT ID is missing; duplicate detection will rely on name and address only"));
        }
        if (vatId.length() < 3) {
            return List.of(RuleViolation.error(ID, FIELD, "VAT ID '%s' is too short".formatted(vatId)));
        }

        String prefix = vatId.substring(0, 2);
        String country = data.address().country();
        if (country != null && IsoCountries.isValid(country) && !prefix.equals(expectedPrefix(country))) {
            return List.of(RuleViolation.error(ID, FIELD,
                    "VAT ID prefix '%s' does not match country %s".formatted(prefix, country)));
        }

        Pattern format = FORMATS.get(prefix);
        if (format == null) {
            return List.of(RuleViolation.warning(ID, FIELD, "No format check available for VAT prefix '%s'".formatted(prefix)));
        }
        if (!format.matcher(vatId).matches()) {
            return List.of(RuleViolation.error(ID, FIELD, "VAT ID '%s' has an invalid format".formatted(vatId)));
        }

        Predicate<String> checksum = CHECKSUMS.get(prefix);
        if (checksum != null && !checksum.test(vatId)) {
            return List.of(RuleViolation.error(ID, FIELD, "VAT ID '%s' has an invalid check digit".formatted(vatId)));
        }
        return List.of();
    }

    /** Greece is the one EU country whose VAT prefix differs from its ISO code. */
    private static String expectedPrefix(String country) {
        return "GR".equals(country) ? "EL" : country;
    }

    /** German USt-IdNr: ISO 7064 MOD 11,10 over the first 8 digits. */
    static boolean isValidGermanVatId(String vatId) {
        String digits = vatId.substring(2);
        int product = 10;
        for (int i = 0; i < 8; i++) {
            int sum = (Character.digit(digits.charAt(i), 10) + product) % 10;
            if (sum == 0) {
                sum = 10;
            }
            product = (2 * sum) % 11;
        }
        int checkDigit = (11 - product) % 10;
        return checkDigit == Character.digit(digits.charAt(8), 10);
    }

    /** Polish NIP: weighted sum of the first 9 digits mod 11 equals the 10th digit. */
    static boolean isValidPolishNip(String vatId) {
        int[] weights = {6, 5, 7, 2, 3, 4, 5, 6, 7};
        String digits = vatId.substring(2);
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i] * Character.digit(digits.charAt(i), 10);
        }
        int checkDigit = sum % 11;
        return checkDigit != 10 && checkDigit == Character.digit(digits.charAt(9), 10);
    }

    /** Swiss UID (CHE-xxx.xxx.xxx): weighted mod 11 over the first 8 digits; 10 is never a valid check digit. */
    static boolean isValidSwissUid(String vatId) {
        int[] weights = {5, 4, 3, 2, 7, 6, 5, 4};
        String digits = vatId.substring(3, 12);
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i] * Character.digit(digits.charAt(i), 10);
        }
        int checkDigit = (11 - sum % 11) % 11;
        return checkDigit != 10 && checkDigit == Character.digit(digits.charAt(8), 10);
    }
}
