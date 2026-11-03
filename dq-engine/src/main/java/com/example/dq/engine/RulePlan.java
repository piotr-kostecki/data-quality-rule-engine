package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The rules of one run, prepared once: filtered, {@code RELEASED} only, and grouped by country so that finding the
 * rules for a record is a map lookup instead of a scan over the catalog.
 */
final class RulePlan {

    private final List<Rule> selected;
    private final Map<String, List<Rule>> applicableByCountry;
    private final Map<String, List<Rule>> skippedByCountry;
    private final List<Rule> applicableElsewhere;
    private final List<Rule> skippedElsewhere;

    private RulePlan(List<Rule> selected) {
        this.selected = List.copyOf(selected);
        Set<String> countries = new TreeSet<>();
        for (Rule rule : selected) {
            if (rule.countryScope() instanceof CountryScope.Countries scoped) {
                countries.addAll(scoped.countries());
            }
        }
        this.applicableByCountry = new HashMap<>();
        this.skippedByCountry = new HashMap<>();
        for (String country : countries) {
            applicableByCountry.put(country, partition(country, true));
            skippedByCountry.put(country, partition(country, false));
        }
        // ASSUMPTION(A-3): a record without a country, or with one no rule is scoped to, gets WORLD rules only.
        // Requires confirmation with architects / BA, see DESIGN.md §10.
        this.applicableElsewhere = partition(null, true);
        this.skippedElsewhere = partition(null, false);
    }

    static RulePlan select(RuleCatalog catalog, RuleFilter filter) {
        RuleFilter released = RuleFilter.status(RuleStatus.RELEASED).and(filter);
        return new RulePlan(catalog.rules().stream().filter(released::test).toList());
    }

    int size() {
        return selected.size();
    }

    List<Rule> applicableTo(@Nullable String country) {
        return country == null ? applicableElsewhere : applicableByCountry.getOrDefault(country, applicableElsewhere);
    }

    List<Rule> skippedFor(@Nullable String country) {
        return country == null ? skippedElsewhere : skippedByCountry.getOrDefault(country, skippedElsewhere);
    }

    private List<Rule> partition(@Nullable String country, boolean applicable) {
        List<Rule> rules = new ArrayList<>();
        for (Rule rule : selected) {
            if (rule.countryScope().includes(country) == applicable) {
                rules.add(rule);
            }
        }
        return List.copyOf(rules);
    }
}
