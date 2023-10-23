package no.nav.melosys.itest

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test


class MyArchitectureTest {

    @Test
    fun architectureRuleSaksflyt() {
        val importedClasses = ClassFileImporter().importPackages(
            "no.nav.melosys.service",
            "no.nav.melosys.integration",
            "no.nav.melosys.domain",
            "no.nav.melosys.repository",
            "no.nav.melosys.tjenester",
            "no.nav.melosys.saksflyt",
        )

        val rule = classes().that().resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAnyPackage("no.nav.melosys.saksflyt..", "..service..", "..domain..")

        rule.check(importedClasses)
    }
}
