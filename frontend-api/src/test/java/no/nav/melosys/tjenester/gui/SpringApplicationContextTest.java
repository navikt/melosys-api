package no.nav.melosys.tjenester.gui;

import no.finn.unleash.Unleash;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Denne testen har kun i oppgave å laste spring context slik att vi kan fange opp spring spesifikke errors tidlig.
 * f.eks (NoSuchBeanException, ConflictingBeanDefinitionException)
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
class SpringApplicationContextTest {

    @MockBean
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private BrevmalListeBygger brevmalListeBygger;
    @MockBean
    private Unleash unleash;
    @MockBean
    private RepresentantTjeneste representantTjeneste;
    @MockBean
    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    @MockBean
    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    @MockBean
    private PersondataFasade persondataFasade;

    @Test
    void contextLaster() {
    }
}
