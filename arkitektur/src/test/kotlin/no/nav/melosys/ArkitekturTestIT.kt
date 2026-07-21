package no.nav.melosys

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
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

    @ArchTest
    val `Fagsak setStatus kalles kun fra FagsakService`: ArchRule = noClasses()
        .that().doNotHaveFullyQualifiedName("no.nav.melosys.service.sak.FagsakService")
        .should().callMethod(Fagsak::class.java, "setStatus", Saksstatuser::class.java)
        .because(
            "fagsakstatus skal endres via FagsakService.oppdaterStatus/avsluttFagsakOgBehandling, " +
                "som tar eksplisitt stilling til saksstatus-synk mot melosys-skjema-api (SkjemaSaksstatusSynk)"
        )
}
