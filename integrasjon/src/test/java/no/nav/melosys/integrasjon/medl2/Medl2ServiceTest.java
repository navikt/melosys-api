package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapMock;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class Medl2ServiceTest {

    private Medl2Service medl2Service;

    @Before
    public void setUp() {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        DokumentFactory dokumentFactory =
                new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        medl2Service = new Medl2Service(medlemskapMock, dokumentFactory);
    }

    @Test
    public void hentPeriodeListe() throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        final String fnr = "77777777773";
        List<Medlemsperiode> medlemsperiodeList = medl2Service.hentPeriodeListe(fnr);
        assertNotNull(medlemsperiodeList);
        assertFalse(medlemsperiodeList.isEmpty());
    }

    @Test
    public void getPeriodeListe() throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        final String fnr = "77777777773";
        Saksopplysning saksopplysning = medl2Service.getPeriodeListe(fnr);
        assertNotNull(saksopplysning);
        // XML is well-formed but lacks 'response' wrapper around 'periodeListe'
        assertNotNull(saksopplysning.getDokumentXml());
    }
}
