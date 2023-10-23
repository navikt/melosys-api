package no.nav.melosys.service

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test


class MyArchitectureTest {

    @Test
    fun architectureRuleService() {
        val importedClasses = ClassFileImporter().importPackages(
            "no.nav.melosys.service",
            "no.nav.melosys.integration",
            "no.nav.melosys.domain",
            "no.nav.melosys.repository",
            "no.nav.melosys.tjenester",
            "no.nav.melosys.saksflyt",
        )

        val rule = classes().that().resideInAPackage("..service..")
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAnyPackage("..service..", "..integration..", "..domain..", "..repository..")


        rule.check(importedClasses)
    }

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

        val rule = classes().that().resideInAPackage("..saksflyt..")
            .should()
            .onlyHaveDependentClassesThat()
//            .resideInAnyPackage("..service..", "..domain..", "..repository..")
            .resideInAnyPackage("..service..", "..domain..")

        rule.check(importedClasses)
    }

//    @Test
//    fun architectureRuleSaksflyt2() {
//        val importedClasses = ClassFileImporter().importPackages(
//            "no.nav.melosys.service",
//            "no.nav.melosys.integration",
//            "no.nav.melosys.domain",
//            "no.nav.melosys.repository",
//            "no.nav.melosys.tjenester",
//            "no.nav.melosys.saksflyt",
//        );
//
//        val rule = classes().that().resideInAPackage("..saksflyt..")
//            .should()
//            .accessClassesThat()
//            .resideInAnyPackage("..service..", "..domain..", "..repository..")
//
//        rule.check(importedClasses);
//    }

    @Test
    fun architectureRuleSaksflyt3() {
        val importedClasses = ClassFileImporter().importPackages(
            "no.nav.melosys.service",
            "no.nav.melosys.integration",
            "no.nav.melosys.domain",
            "no.nav.melosys.repository",
            "no.nav.melosys.tjenester",
            "no.nav.melosys.saksflyt",
        )

        val rule = noClasses().that().resideInAPackage("..saksflyt..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..integration..")

        rule.check(importedClasses)
    }

    @Test
    fun some_architecture_rule_2() {
        val importedClasses = ClassFileImporter().importPackages(
            "no.nav.melosys.service",
            "no.nav.melosys.integration",
            "no.nav.melosys.domain",
            "no.nav.melosys.repository",
            "no.nav.melosys.tjenester",
            "no.nav.melosys.saksflyt",
        )

        val rule = classes().that().resideInAPackage("..service..")
            .should()
            .onlyBeAccessed()
            .byAnyPackage("..service..")
//            .byAnyPackage("..service..", "..integration..", "..domain..", "..tjenester..");

        rule.check(importedClasses)
    }
}
