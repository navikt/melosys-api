package no.nav.melosys.itest

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction
import com.tngtech.archunit.lang.syntax.elements.ClassesThat
import org.junit.jupiter.api.TestInstance

@AnalyzeClasses(packages = ["no.nav.melosys.."])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyArchitectureTest {

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
                "no.nav.melosys.exception.."
            )

    @ArchTest
    val `Saksfly skal bruke COMMON_PACKAGES` =
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
            "com.google..",
            "org.springframework..",
            "kotlin..",
            "com.fasterxml..",
            "io.getunleash..",
            "org.slf4j..",
            "org.jetbrains..",
            "org.apache.commons..",
            "mu..",
            "io.micrometer..",
            "no.nav.security.."
        )
    }
}
