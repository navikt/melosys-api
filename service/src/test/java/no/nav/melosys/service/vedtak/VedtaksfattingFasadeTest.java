package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VedtaksfattingFasadeTest {

    private static final long behandlingID = 1L;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private EosVedtakService mockEosVedtakService;

    @Mock
    private FtrlVedtakService mockFtrlVedtakService;

    @Mock
    private TrygdeavtaleVedtakService trygdeavtaleVedtakService;

    @Mock
    private ÅrsavregningVedtakService årsavregningVedtakService;

    private VedtaksfattingFasade vedtaksfattingFasade;

    private Behandling behandling;

    @BeforeEach
    void init() {
        vedtaksfattingFasade = new VedtaksfattingFasade(mockBehandlingService, new FattVedtakVelger(mockEosVedtakService, mockFtrlVedtakService, trygdeavtaleVedtakService, årsavregningVedtakService));
        behandling = lagBehandling();

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_feilBehandlingstema_kasterException() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        FattVedtakRequest fattVedtakRequest = lagFattFtrlVedtakRequest();

        assertThatThrownBy(() -> vedtaksfattingFasade.fattVedtak(behandlingID, fattVedtakRequest))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Kan ikke fatte vedtak ved behandlingstema");
    }

    @Test
    void fattVedtak_EU_EOS_skalKalleEosVedtakService() {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattEosVedtakRequest());

        verify(mockEosVedtakService).fattVedtak(eq(behandling), any(FattVedtakRequest.class));
        verifyNoInteractions(mockFtrlVedtakService);
    }

    @Test
    void fattVedtak_delvisAutomatisert_skalKalleEosVedtakSystemService() {
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK).build();


        vedtaksfattingFasade.fattVedtak(behandlingID, request);


        verify(mockEosVedtakService).fattVedtak(argThat(behandling1 ->
            behandling1.getId() == behandlingID), argThat(fattVedtakRequest ->
            fattVedtakRequest.getVedtakstype() == FØRSTEGANGSVEDTAK
                && fattVedtakRequest.getBehandlingsresultatTypeKode() == FASTSATT_LOVVALGSLAND));
        verifyNoInteractions(mockFtrlVedtakService);
    }

    @Test
    void fattVedtak_FTRL_skalKalleFtrlVedtakService() {
        setFagsakPåBehandling(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattFtrlVedtakRequest());

        verify(mockFtrlVedtakService).fattVedtak(eq(behandling), any(FattVedtakRequest.class));
        verifyNoInteractions(mockEosVedtakService);
    }

    @Test
    void fattVedtak_TRYGDEAVTALER_kasterException() {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattTrygdeavtaleVedtakRequest());

        verify(trygdeavtaleVedtakService).fattVedtak(eq(behandling), any(FattVedtakRequest.class));
        verifyNoInteractions(mockEosVedtakService);
    }

    private Behandling lagBehandling() {
        Behandling nyBehandling = BehandlingTestFactory.builderWithDefaults().build();
        nyBehandling.setId(behandlingID);
        nyBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        nyBehandling.setType(Behandlingstyper.FØRSTEGANG);
        nyBehandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(nyBehandling);

        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        nyBehandling.setFagsak(fagsak);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        return nyBehandling;
    }

    private void setFagsakPåBehandling(Sakstyper sakstype) {
        behandling.setFagsak(FagsakTestFactory.builder().type(sakstype).build());
    }

    private FattVedtakRequest lagFattEosVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("Fritekst")
            .build();
    }

    private FattVedtakRequest lagFattFtrlVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("Begrunnelse")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
    }

    private FattVedtakRequest lagFattTrygdeavtaleVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("Begrunnelse")
            .build();
    }
}
