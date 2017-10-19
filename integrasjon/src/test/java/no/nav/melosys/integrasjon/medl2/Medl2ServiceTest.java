package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Medl2ServiceTest {

    private Medl2Service medl2Service;

    @Before
    public void setUp() {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        medl2Service = new Medl2Service(medlemskapMock);
    }

    @Test
    public void getPeriodeListe() throws IntegrasjonException, SikkerhetsbegrensningException {
        final String fnr = "77777777773";
        Saksopplysning saksopplysning = medl2Service.getPeriodeListe(fnr);
        assertNotNull(saksopplysning);
        // XML is well-formed but lacks 'response' wrapper around 'periodeListe'
        assertNotNull(saksopplysning.getDokumentXml());
        // TODO: Testing mapping til intern modell når EESSI2-335 er på plass
    }
}
