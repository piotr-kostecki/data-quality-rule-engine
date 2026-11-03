/**
 * Data-quality rule engine: runs a catalog of rules over a stream of records and emits one explained result
 * per applicable rule and record, plus isolated errors and a run summary.
 *
 * <p>Only {@code com.example.dq.engine} is API. Rule logic is pluggable through {@link com.example.dq.engine.RuleLogic};
 * the {@code dq-engine-cel} module provides CEL expressions.
 */
module com.example.dq.engine {
    requires transitive org.jspecify;

    exports com.example.dq.engine;
}
