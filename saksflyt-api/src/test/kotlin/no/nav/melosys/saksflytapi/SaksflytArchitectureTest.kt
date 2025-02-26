package no.nav.melosys.saksflytapi

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction
import com.tngtech.archunit.lang.syntax.elements.ClassesThat
import org.junit.jupiter.api.TestInstance

@AnalyzeClasses(packages = ["no.nav.melosys.saksflytapi.."], importOptions = [ImportOption.DoNotIncludeTests::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaksflytapiArchitectureTest {

    @ArchTest
    val `Saksflytapi skal ikke brukes av andre moduler` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflytapi..")
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage("no.nav.melosys.saksflyt..","no.nav.melosys.saksflytapi..", "no.nav.melosys.service..")

    @ArchTest
    val `Saksflytapi skal bare være avhengig av` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflytapi..")
            .should()
            .onlyDependOnClassesThat()
            .resideInPackagesIncludingCommon(
                "no.nav.melosys.domain..",
                "no.nav.melosys.exception..",
                "no.nav.melosys.sikkerhet..",
                "no.nav.melosys.saksflytapi..",
                "no.nav.melosys.config..",
            )

    @ArchTest
    //Denne går gjennom med libs i common package som ikke blir brukt
    val `Saksflytapi skal bruke COMMON_PACKAGES` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflytapi..")
            .should()
            .dependOnClassesThat()
            .resideInPackagesIncludingCommon()


    private fun ClassesThat<ClassesShouldConjunction>.resideInPackagesIncludingCommon(vararg packages: String): ClassesShouldConjunction {
        return this.resideInAnyPackage(*commonPackagesAnd(*packages))
    }

    private fun commonPackagesAnd(vararg packages: String): Array<String> {
        return (packages.toList() + COMMON_PACKAGES).toTypedArray()
    }

    companion object {
        private val COMMON_PACKAGES = listOf(
            "java..",
            "javax..",
            "jakarta..",
            "kotlin..",
            "mu..",
            "com..",
            "org..",
            "io..",
            "no.nav.security.."
        )
    }
}
