package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MedlServiceTest {

    private MedlService medlService;

    @Before
    public void setUp() {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        medlService = new MedlService(medlemskapMock, dokumentFactory);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void getPeriodeListe() throws IntegrasjonException, SikkerhetsbegrensningException {
        final String fnr = "77777777773";
        Saksopplysning saksopplysning = medlService.hentPeriodeListe(fnr, null, null);
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
