package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Medl2ServiceTest {

    private Medl2Service medl2Service;

    @Before
    public void setUp() {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        medl2Service = new Medl2Service(medlemskapMock, dokumentFactory);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void getPeriodeListe() throws IntegrasjonException, SikkerhetsbegrensningException {
        final String fnr = "77777777773";
        Saksopplysning saksopplysning = medl2Service.getPeriodeListe(fnr);
        assertNotNull(saksopplysning);
        assertNotNull(saksopplysning.getDokumentXml());

        MedlemskapDokument medlemskapDokument = (MedlemskapDokument) saksopplysning.getDokument();

        assertNotNull(medlemskapDokument);
        assertNotNull(medlemskapDokument.getMedlemsperiode());
        assertFalse(medlemskapDokument.getMedlemsperiode().isEmpty());

        for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
            assertNotNull(medlemsperiode.getType());
            assertNotNull(medlemsperiode.getStatus());
            assertNotNull(medlemsperiode.getLovvalg());
            assertNotNull(medlemsperiode.getKilde());
            assertNotNull(medlemsperiode.getGrunnlagstype());
        }
    }
}
