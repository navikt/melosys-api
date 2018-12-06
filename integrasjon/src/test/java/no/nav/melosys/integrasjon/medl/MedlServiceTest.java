package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class MedlServiceTest {

    private MedlService medlService;

    private String fnr = "77777777773";

    private Lovvalgsperiode lovvalgsperiode;

    private MedlService medlServiceSpy;

    @Before
    public void setUp() throws PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        BehandleMedlemskapConsumer behandleMedlemskapConsumer = mock(BehandleMedlemskapConsumer.class);
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        medlService = new MedlService(medlemskapMock, behandleMedlemskapConsumer, dokumentFactory);

        lovvalgsperiode = new Lovvalgsperiode();

        OpprettPeriodeResponse response = new OpprettPeriodeResponse();
        response.setPeriodeId(123L);

        medlServiceSpy = spy(medlService);
        when(behandleMedlemskapConsumer.opprettPeriode(ArgumentMatchers.any(OpprettPeriodeRequest.class))).thenReturn(response);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void getPeriodeListe() throws MelosysException {
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

    @Test
    public void opprettPeriodeSomEndelig() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        medlServiceSpy.opprettPeriodeEndelig(fnr, lovvalgsperiode);
        verify(medlServiceSpy, times(1)).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.GYLD), same(LovvalgMedl.ENDL));
    }

    @Test
    public void opprettPeriodeSomUnderAvklaring() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        medlServiceSpy.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode);
        verify(medlServiceSpy, times(1)).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.UAVK), same(LovvalgMedl.UAVK));
    }
}
