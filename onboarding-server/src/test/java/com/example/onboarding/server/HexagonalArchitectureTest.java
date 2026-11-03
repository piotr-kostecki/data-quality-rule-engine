package com.example.onboarding.server;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Module boundaries are enforced by Gradle (the library cannot see the application, the application cannot
 * see Spring). These rules guard what Gradle cannot: the direction of dependencies between packages.
 */
@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule hexagon = onionArchitecture()
            .domainModels("com.example.onboarding.application.domain..", "com.example.partnerquality..")
            .applicationServices("com.example.onboarding.application.service..", "com.example.onboarding.application.port..")
            .adapter("web", "com.example.onboarding.server.adapter.in.web..")
            .adapter("persistence", "com.example.onboarding.server.adapter.out.persistence..")
            .adapter("event", "com.example.onboarding.server.adapter.out.event..")
            // the library is a self-contained domain model; there is no separate domain-service layer
            .withOptionalLayers(true)
            // The composition root wires everything together, so it may depend on every layer
            .ignoreDependency(resideInAPackage("com.example.onboarding.server.config.."), DescribedPredicate.alwaysTrue());

    @ArchTest
    static final ArchRule adaptersUsePortsNotServices = noClasses()
            .that().resideInAPackage("com.example.onboarding.server.adapter..")
            .should().dependOnClassesThat().resideInAPackage("com.example.onboarding.application.service..")
            .because("adapters talk to the application through ports only; config is the only place that knows the service");

    @ArchTest
    static final ArchRule drivingAdaptersOnlyUseIncomingPorts = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..port.out..");

    @ArchTest
    static final ArchRule nobodyUsesLibraryInternals = noClasses()
            .that().resideOutsideOfPackage("com.example.partnerquality..")
            .should().dependOnClassesThat().resideInAPackage("com.example.partnerquality.internal..")
            .because("only exported packages of partner-quality are API");
}
