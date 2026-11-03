package com.example.partnerquality.internal.text;

import org.jspecify.annotations.Nullable;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reduces a company name to the part that identifies the company, so that
 * "Müller Logistik GmbH", "MUELLER-LOGISTIK" and "Muller Logistik" end up close to each other.
 */
public final class NameNormalizer {

    /** Legal-form suffixes, written as they look after dots are removed ("sp. z o.o." becomes "sp z oo"). */
    private static final Pattern LEGAL_FORMS = Pattern.compile(
            "\\b(sp z o ?o|gmbh co kg|gmbh|ag|se|kg|sa|spa|sarl|bv|nv|ltd|limited|inc|llc|plc|corp)\\b");

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NameNormalizer() {
    }

    public static String normalize(@Nullable String name) {
        if (name == null) {
            return "";
        }
        String s = Normalizer.normalize(name, Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace('ł', 'l');
        s = DIACRITICS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        s = s.replace(".", "");
        s = NON_ALPHANUMERIC.matcher(s).replaceAll(" ").strip();

        String withoutLegalForm = WHITESPACE.matcher(LEGAL_FORMS.matcher(s).replaceAll(" ")).replaceAll(" ").strip();
        // A company literally called "AG" should not normalize to an empty string.
        return withoutLegalForm.isEmpty() ? s : withoutLegalForm;
    }
}
