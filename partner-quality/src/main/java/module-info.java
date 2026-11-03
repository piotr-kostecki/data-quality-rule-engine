/**
 * Business-partner data quality: validation rules and duplicate detection.
 *
 * <p>Only the three exported packages are public API. Everything under {@code internal} may change in any
 * release, and the module system prevents modular consumers from using it.
 */
module com.example.partnerquality {
    requires transitive org.jspecify;

    exports com.example.partnerquality;
    exports com.example.partnerquality.validation;
    exports com.example.partnerquality.matching;
}
