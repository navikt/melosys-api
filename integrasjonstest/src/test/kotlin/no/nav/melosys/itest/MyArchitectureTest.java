package no.nav.melosys.itest;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.apache.commons.lang3.ArrayUtils;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "no.nav.melosys..")
public class MyArchitectureTest {

    private static final String[] COMMON_PACKAGES = {
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
        "no.nav.security.."};
    @ArchTest
    final ArchRule saksflytAksess =
        classes().that().resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage("no.nav.melosys.saksflyt..", "no.nav.melosys.itest..")
            .as("Saksflyt skal ikke brukes av andre moduler");

    @ArchTest
    final ArchRule saksflytDependencies =
        classes().that().resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(commonPackagesAnd("no.nav.melosys.saksflyt..", "no.nav.melosys.saksflytapi..", "no.nav.melosys.domain..", "no.nav.melosys.service..", "no.nav.melosys.integrasjon..", "no.nav.melosys.sikkerhet..", "no.nav.melosys.exception.."));

    @ArchTest
    final ArchRule saksflytCommonLibs =
        classes().that().resideInAPackage("no.nav.melosys.saksflyt..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(commonPackagesAnd());

    private static String[] commonPackagesAnd(String... packages) {
        return ArrayUtils.addAll(packages, COMMON_PACKAGES);
    }
}
