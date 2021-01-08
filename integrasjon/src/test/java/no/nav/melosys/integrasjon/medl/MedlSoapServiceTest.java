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

public class MedlSoapServiceTest {

    private MedlSoapService medlSoapService;

    private String fnr = "77777777773";

    private Lovvalgsperiode lovvalgsperiode;

    private MedlSoapService medlSoapServiceSpy;
    private BehandleMedlemskapV2 behandleMedlemskapV2;

    @Before
    public void setUp() throws PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        behandleMedlemskapV2 = mock(BehandleMedlemskapV2.class);
        BehandleMedlemskapConsumerImpl behandleMedlemskapConsumer1 = new BehandleMedlemskapConsumerImpl(behandleMedlemskapV2);
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());

        medlSoapService = new MedlSoapService(medlemskapMock, behandleMedlemskapConsumer1, dokumentFactory);

        lovvalgsperiode = new Lovvalgsperiode();

        OpprettPeriodeResponse response = new OpprettPeriodeResponse();
        response.setPeriodeId(123L);

        medlSoapServiceSpy = spy(medlSoapService);
        when(((BehandleMedlemskapConsumer) behandleMedlemskapConsumer1).opprettPeriode(ArgumentMatchers.any(OpprettPeriodeRequest.class))).thenReturn(response);
    }

    @Test
    public void getPeriodeListe() throws MelosysException {
        Saksopplysning saksopplysning = medlSoapService.hentPeriodeListe(fnr, null, null);
        assertNotNull(saksopplysning);
        assertNotNull(saksopplysning.getKilder());
        assertFalse(saksopplysning.getKilder().isEmpty());
        assertNotNull(saksopplysning.getKilder().iterator().next().getMottattDokument());

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
        medlSoapServiceSpy.opprettPeriodeEndelig(fnr, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        verify(medlSoapServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.GYLD), same(LovvalgMedl.ENDL), same(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void opprettPeriodeSomUnderAvklaring() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        medlSoapServiceSpy.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode, KildedokumenttypeMedl.SED);
        verify(medlSoapServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.UAVK), same(LovvalgMedl.UAVK), same(KildedokumenttypeMedl.SED));
    }

    @Test
    public void oppdaterPeriodeSomEndelig_lagerRequestMedRiktigInformasjon() throws FunksjonellException, TekniskException, PeriodeIkkeFunnet, PeriodeUtdatert, UgyldigInput, Sikkerhetsbegrensning {
        long periodeId = 10L;
        lovvalgsperiode.setMedlPeriodeID(periodeId);

        ArgumentCaptor<OppdaterPeriodeRequest> captor = ArgumentCaptor.forClass(OppdaterPeriodeRequest.class);

        medlSoapServiceSpy.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        verify(behandleMedlemskapV2).oppdaterPeriode(captor.capture());

        OppdaterPeriodeRequest oppdaterPeriodeRequest = captor.getValue();

        assertThat(oppdaterPeriodeRequest.getPeriodeId()).isEqualTo(periodeId);
        assertThat(oppdaterPeriodeRequest.getVersjon()).isZero();
    }

    @Test
    public void avvisPeriode_senderRequestMedKorrektAarsak() throws Exception {
        long periodeId = 10L;

        ArgumentCaptor<AvvisPeriodeRequest> captor = ArgumentCaptor.forClass(AvvisPeriodeRequest.class);

        medlSoapService.avvisPeriode(periodeId, StatusaarsakMedl.OPPHORT);
        verify(behandleMedlemskapV2).avvisPeriode(captor.capture());

        AvvisPeriodeRequest request = captor.getValue();
        assertThat(request.getPeriodeId()).isEqualTo(periodeId);
        assertThat(request.getAarsak().getValue()).isEqualTo(StatusaarsakMedl.OPPHORT.getKode());
    }
}
