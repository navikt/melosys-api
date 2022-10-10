package no.nav.melosys.service.vedtak;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
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
import static no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    private VedtaksfattingFasade vedtaksfattingFasade;

    private Behandling behandling;
    private final FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    void init() {
        vedtaksfattingFasade = new VedtaksfattingFasade(mockBehandlingService, mockEosVedtakService, mockFtrlVedtakService, trygdeavtaleVedtakService, unleash);
        behandling = lagBehandling();

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_feilBehandlingstype_kasterException() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        FattVedtakRequest fattVedtakRequest = lagFattFtrlVedtakRequest();

        assertThatThrownBy(() -> vedtaksfattingFasade.fattVedtak(behandlingID, fattVedtakRequest))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Kan ikke fatte vedtak ved behandlingstema UFM: Melding om utstasjonering – A009");
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

        vedtaksfattingFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND);

        verify(mockEosVedtakService).fattVedtak(behandling, FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK);
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

    @Test
    void endreVedtak_EU_EOS_skalKalleEosVedtakService() {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        vedtaksfattingFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null);

        verify(mockEosVedtakService).endreVedtaksperiode(eq(behandling), eq(ENDRINGER_ARBEIDSSITUASJON), isNull(), isNull());
    }

    @Test
    void endreVedtak_FTRL_kasterException() {
        setFagsakPåBehandling(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtaksfattingFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksendring for sakstype FTRL er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
    }

    @Test
    void endreVedtak_TRYGDEAVTALER_kasterException() {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtaksfattingFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksendring for sakstype TRYGDEAVTALE er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakService);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        return behandling;
    }

    private void setFagsakPåBehandling(Sakstyper sakstype) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        behandling.setFagsak(fagsak);
    }

    private FattVedtakRequest lagFattEosVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("Fritekst")
            .build();
    }

    private FattVedtakRequest lagFattFtrlVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("Begrunnelse")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
    }

    private FattVedtakRequest lagFattTrygdeavtaleVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("Begrunnelse")
            .build();
    }
}
