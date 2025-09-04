package no.nav.melosys

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.TestInstance

@AnalyzeClasses(packages = ["no.nav.melosys"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArkitekturTestIT {

    @ArchTest
    val `Melosys has layered architecture`: ArchRule = layeredArchitecture()
        .consideringOnlyDependenciesInAnyPackage("no.nav.melosys")
        .layer("Controller").definedBy("..tjenester.gui..")
        .layer("Service").definedBy("..service..")
        .layer("Integrations").definedBy("..integrasjon..")
        .layer("Persistence").definedBy("..repository..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Integrations").mayOnlyBeAccessedByLayers("Service")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
}
