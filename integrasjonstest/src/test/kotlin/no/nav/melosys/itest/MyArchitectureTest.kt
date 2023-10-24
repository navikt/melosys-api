package no.nav.melosys.itest

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class MyArchitectureTest {

    @Disabled("WIP testen tester ikke det som skal testes")
    @Test
    fun architectureRuleSaksflyt() {
        val importedClasses = ClassFileImporter().importPackages(
            "no.nav.melosys.service",
            "no.nav.melosys.integration",
            "no.nav.melosys.domain",
            "no.nav.melosys.repository",
            "no.nav.melosys.tjenester",
            "no.nav.melosys.saksflyt",
            "no.nav.melosys.saksflytapi"
        )


        val rule = classes().that().resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("no.nav.melosys.service..", "no.nav.melosys.domain..", "no.nav.melosys.saksflytapi")

        rule.check(importedClasses)
    }
}
