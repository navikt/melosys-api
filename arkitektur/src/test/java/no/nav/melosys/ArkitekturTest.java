package no.nav.melosys;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "no.nav.melosys")
class ArkitekturTest {

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


    private static final ArchCondition<JavaClass> notBeUsingStrictnessLenient =
        new ArchCondition<>("not be using lenient strictness") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
                var mockitoSettings = javaClass.tryGetAnnotationOfType(MockitoSettings.class);
                if (mockitoSettings.isPresent() && mockitoSettings.get().strictness() == Strictness.LENIENT) {
                    var error = String.format(
                        "Test-class %s is using @MockitoSettings with LENIENT strictness", javaClass.getSimpleName()
                    );
                    conditionEvents.add(SimpleConditionEvent.violated(javaClass, error));
                }
            }
        };

    @ArchTest
    static final ArchRule do_not_use_lenient_strictness_on_tests = FreezingArchRule.freeze(
        classes().that().areAnnotatedWith(MockitoSettings.class).should(notBeUsingStrictnessLenient)
    );
}
