package no.nav.melosys.saksflyt

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction
import com.tngtech.archunit.lang.syntax.elements.ClassesThat
import org.junit.jupiter.api.TestInstance

@AnalyzeClasses(packages = ["no.nav.melosys.saksflyt.."], importOptions = [ImportOption.DoNotIncludeTests::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaksflytArchitectureTest {

    @ArchTest
    val `Saksflyt skal ikke brukes av andre moduler` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage("no.nav.melosys.saksflyt..", "no.nav.melosys.itest..")

    @ArchTest
    val `Saksflyt skal bare være avhgengig av` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyDependOnClassesThat()
            .resideInPackagesIncludingCommon(
                "no.nav.melosys.saksflyt..",
                "no.nav.melosys.saksflytapi..",
                "no.nav.melosys.domain..",
                "no.nav.melosys.service..",
                "no.nav.melosys.integrasjon..",
                "no.nav.melosys.sikkerhet..",
                "no.nav.melosys.exception..",
                "no.nav.melosys.config.."
            )

    @ArchTest
    //Denne går gjennom med libs i common package som ikke blir brukt
    val `Saksflyt skal bruke COMMON_PACKAGES` =
        classes().that()
            .resideInAPackage("no.nav.melosys.saksflyt..")
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
            "kotlin..",
            "lol..",
            "mu..",
            "com..",
            "org..",
            "io..",
            "no.nav.security.."
        )
    }
}
