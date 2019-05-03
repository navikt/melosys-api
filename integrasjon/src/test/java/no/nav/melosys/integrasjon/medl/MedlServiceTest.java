package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumerImpl;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.AvvisPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class MedlServiceTest {

    private MedlService medlService;

    private String fnr = "77777777773";

    private Lovvalgsperiode lovvalgsperiode;

    private MedlService medlServiceSpy;
    private BehandleMedlemskapConsumerImpl behandleMedlemskapConsumer;
    private BehandleMedlemskapV2 behandleMedlemskapV2;

    @Before
    public void setUp() throws PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        behandleMedlemskapV2 = mock(BehandleMedlemskapV2.class);
        behandleMedlemskapConsumer = new BehandleMedlemskapConsumerImpl(behandleMedlemskapV2);
        BehandleMedlemskapConsumer behandleMedlemskapConsumer = this.behandleMedlemskapConsumer;
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
    public void opprettPeriodeSomEndelig() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        medlServiceSpy.opprettPeriodeEndelig(fnr, lovvalgsperiode);
        verify(medlServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.GYLD), same(LovvalgMedl.ENDL));
    }

    @Test
    public void opprettPeriodeSomUnderAvklaring() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        medlServiceSpy.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode);
        verify(medlServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.UAVK), same(LovvalgMedl.UAVK));
    }

    @Test
    public void oppdaterPeriodeSomEndelig_lagerRequestMedRiktigInformasjon() throws FunksjonellException, TekniskException, PeriodeIkkeFunnet, PeriodeUtdatert, UgyldigInput, Sikkerhetsbegrensning, no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning {
        long periodeId = 10L;
        lovvalgsperiode.setMedlPeriodeID(periodeId);

        ArgumentCaptor<OppdaterPeriodeRequest> captor = ArgumentCaptor.forClass(OppdaterPeriodeRequest.class);

        medlServiceSpy.oppdaterPeriodeEndelig(lovvalgsperiode);
        verify(behandleMedlemskapV2).oppdaterPeriode(captor.capture());

        OppdaterPeriodeRequest oppdaterPeriodeRequest = captor.getValue();

        assertThat(oppdaterPeriodeRequest.getPeriodeId()).isEqualTo(periodeId);
        assertThat(oppdaterPeriodeRequest.getVersjon()).isEqualTo(0);
    }

    @Test
    public void avvisPeriode_senderRequestMedKorrektAarsak() throws Exception {
        long periodeId = 10L;
        lovvalgsperiode.setMedlPeriodeID(periodeId);

        ArgumentCaptor<AvvisPeriodeRequest> captor = ArgumentCaptor.forClass(AvvisPeriodeRequest.class);

        medlService.avvisPeriode(lovvalgsperiode, StatusaarsakMedl.OPPHORT);
        verify(behandleMedlemskapV2).avvisPeriode(captor.capture());

        AvvisPeriodeRequest request = captor.getValue();
        assertThat(request.getPeriodeId()).isEqualTo(periodeId);
        assertThat(request.getAarsak().getValue()).isEqualTo(StatusaarsakMedl.OPPHORT.getKode());
    }
}
