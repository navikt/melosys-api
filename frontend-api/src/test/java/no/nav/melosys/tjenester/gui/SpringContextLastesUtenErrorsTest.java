package no.nav.melosys.tjenester.gui;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Denne testen har kun i oppgave å laste spring context slik att vi kan fange opp spring spesifikke errors tidlig.
 * f.eks (NoSuchBeanException, ConflictingBeanDefinitionException)
 */
@SpringBootTest
class SpringContextLastesUtenErrorsTest {

    @Test
    void contextLaster() {
    }

}
