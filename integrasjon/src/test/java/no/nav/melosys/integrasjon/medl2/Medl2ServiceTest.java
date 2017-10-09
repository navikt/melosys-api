package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapMock;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class Medl2ServiceTest {

    private Medl2Service medl2Service;

    @Before
    public void setUp() {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        medl2Service = new Medl2Service(medlemskapMock);
    }

    @Test
    public void testHentPeriodeListe() throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        final String fnr = "77777777773";
        List<Medlemsperiode> medlemsperiodeList = medl2Service.hentPeriodeListe(fnr);
        assertNotNull(medlemsperiodeList);
    }
}
