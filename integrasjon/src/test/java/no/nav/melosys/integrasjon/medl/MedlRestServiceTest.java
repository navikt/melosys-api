package no.nav.melosys.integrasjon.medl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.behandle.BehandleMedlemskapConsumerImpl;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.AvvisPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class MedlRestServiceTest {

    private MedlRestService medlRestService;

    private String fnr = "77777777773";

    private Lovvalgsperiode lovvalgsperiode;
    private MedlRestService medlRestServiceSpy;
    private BehandleMedlemskapV2 behandleMedlemskapV2;
    private MedlemskapRestConsumer mockRestConsumer;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MedlemskapMock medlemskapMock = new MedlemskapMock();
        behandleMedlemskapV2 = mock(BehandleMedlemskapV2.class);
        BehandleMedlemskapConsumerImpl behandleMedlemskapConsumer1 = new BehandleMedlemskapConsumerImpl(behandleMedlemskapV2);
        mockRestConsumer = mock(MedlemskapRestConsumer.class);

        medlRestService = new MedlRestService(medlemskapMock, behandleMedlemskapConsumer1, mockRestConsumer, objectMapper);

        lovvalgsperiode = new Lovvalgsperiode();

        OpprettPeriodeResponse response = new OpprettPeriodeResponse();
        response.setPeriodeId(123L);

        medlRestServiceSpy = spy(medlRestService);
        when(((BehandleMedlemskapConsumer) behandleMedlemskapConsumer1).opprettPeriode(ArgumentMatchers.any(OpprettPeriodeRequest.class))).thenReturn(response);
    }

    @Test
    public void getPeriodeListe() throws Exception {
        when(mockRestConsumer.hentPeriodeListe(eq(fnr), any(), any())).thenReturn(
            asList(objectMapper.readValue(
                getClass().getClassLoader().getResource("mock/medlemskap/" + fnr + ".json"),
                MedlemskapsunntakForGet[].class)
            )
        );

        Saksopplysning saksopplysning = medlRestService.hentPeriodeListe(fnr, null, null);
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
        medlRestServiceSpy.opprettPeriodeEndelig(fnr, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        verify(medlRestServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.GYLD), same(LovvalgMedl.ENDL), same(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void opprettPeriodeSomUnderAvklaring() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        medlRestServiceSpy.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode, KildedokumenttypeMedl.SED);
        verify(medlRestServiceSpy).opprettPeriode(same(fnr), same(lovvalgsperiode), same(PeriodestatusMedl.UAVK), same(LovvalgMedl.UAVK), same(KildedokumenttypeMedl.SED));
    }

    @Test
    public void oppdaterPeriodeSomEndelig_lagerRequestMedRiktigInformasjon() throws FunksjonellException, TekniskException, PeriodeIkkeFunnet, PeriodeUtdatert, UgyldigInput, Sikkerhetsbegrensning {
        long periodeId = 10L;
        lovvalgsperiode.setMedlPeriodeID(periodeId);

        ArgumentCaptor<OppdaterPeriodeRequest> captor = ArgumentCaptor.forClass(OppdaterPeriodeRequest.class);

        medlRestServiceSpy.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        verify(behandleMedlemskapV2).oppdaterPeriode(captor.capture());

        OppdaterPeriodeRequest oppdaterPeriodeRequest = captor.getValue();

        assertThat(oppdaterPeriodeRequest.getPeriodeId()).isEqualTo(periodeId);
        assertThat(oppdaterPeriodeRequest.getVersjon()).isZero();
    }

    @Test
    public void avvisPeriode_senderRequestMedKorrektAarsak() throws Exception {
        long periodeId = 10L;

        ArgumentCaptor<AvvisPeriodeRequest> captor = ArgumentCaptor.forClass(AvvisPeriodeRequest.class);

        medlRestService.avvisPeriode(periodeId, StatusaarsakMedl.OPPHORT);
        verify(behandleMedlemskapV2).avvisPeriode(captor.capture());

        AvvisPeriodeRequest request = captor.getValue();
        assertThat(request.getPeriodeId()).isEqualTo(periodeId);
        assertThat(request.getAarsak().getValue()).isEqualTo(StatusaarsakMedl.OPPHORT.getKode());
    }
}
