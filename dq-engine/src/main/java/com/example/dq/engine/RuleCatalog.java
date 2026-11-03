package com.example.dq.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where rules come from: code, a file, a rule-management service. Hosts implement this to plug in their own
 * source; {@link #of(List)} covers rules built in code. A catalog must not contain two rules with the same id.
 */
@FunctionalInterface
public interface RuleCatalog {

    /** All rules in catalog order, whatever their status. */
    List<Rule> rules();

    /** Catalog of the given rules; fails fast on duplicate ids. */
    static RuleCatalog of(List<Rule> rules) {
        List<Rule> copy = List.copyOf(rules);
        Set<String> ids = new HashSet<>();
        for (Rule rule : copy) {
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("Duplicate rule id in catalog: " + rule.id());
            }
        }
        return () -> copy;
    }

    static RuleCatalog of(Rule... rules) {
        return of(List.of(rules));
    }
}
