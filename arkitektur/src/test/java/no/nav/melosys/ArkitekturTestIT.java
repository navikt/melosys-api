package no.nav.melosys;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "no.nav.melosys")
class ArkitekturTestIT {

    @ArchTest
    static final ArchRule melosys_has_layered_architecture = layeredArchitecture()
        .layer("Controller").definedBy("..tjenester.gui..")
        .layer("Service").definedBy("..service..")
        .layer("Integrations").definedBy("..integrasjon..")
        .layer("Persistence").definedBy("..repository..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Integrations").mayOnlyBeAccessedByLayers("Service")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");
}
