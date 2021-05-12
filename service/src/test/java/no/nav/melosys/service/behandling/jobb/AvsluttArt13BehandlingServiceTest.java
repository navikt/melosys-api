package no.nav.melosys.service.behandling.jobb;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvsluttArt13BehandlingServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private AvsluttArt13BehandlingService avsluttArt13BehandlingService;

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = new Behandling();
    private final Fagsak fagsak = new Fagsak();
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final VedtakMetadata vedtakMetadata = new VedtakMetadata();

    private final long behandlingID = 11L;

    @BeforeEach
    public void setup() {
        avsluttArt13BehandlingService = new AvsluttArt13BehandlingService(behandlingService, fagsakService,
            behandlingsresultatService, medlPeriodeService, lovvalgsperiodeService);

        behandling.setId(behandlingID);
        behandlingsresultat.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        behandling.setFagsak(fagsak);
        fagsak.setSaksnummer("MEL-11");
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A);
        lovvalgsperiode.setMedlPeriodeID(123L);

        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID))
            .thenReturn(behandlingsresultat);
    }

    @Test
    void avsluttBehandlingArt13_ikkeArt13_kasterException() {
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID))
            .withMessageContaining("Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse");
    }

    @Test
    void avsluttBehandlingArt13_søknad1MndSidenVedtak_behandlingIkkeAvlsuttet() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(1, 0));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService, never()).avsluttFagsakOgBehandling(any(), any());
    }

    @Test
    void avsluttBehandlingArt13_norgeUtpekt2Mnd1DagSidenVedtak_behandlingBlirAvlsuttet() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(true));
    }

    @Test
    void avsluttBehandlingArt13_norgeUtpektVedtakIkkeLagret_kasterException() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandlingsresultat.setVedtakMetadata(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID))
            .withMessageContaining("har ikke et vedtak og status kan da ikke settes til AVSLUTTET");
    }

    @Test
    void avsluttBehandlingArt13_søknad2Mnd1DagSidenEndretDato_medlOppdatertOgBehandlingBlirAvsluttet() {
        behandlingsresultat.setEndretDato(månederOgDagerSiden(2, 1));
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void avsluttBehandlingArt13_søknad3MndSidenEndretDatoUtpekingUtenVedtak_lovvalgsperiodeOpprettetOgBehandlingAvsluttet() {

        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(
            LocalDate.now(), LocalDate.now(), Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null);
        utpekingsperiode.setMedlPeriodeID(123L);

        behandlingsresultat.setEndretDato(månederOgDagerSiden(3, 0));
        behandlingsresultat.setVedtakMetadata(null);
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(), anyCollection())).thenAnswer(a -> a.getArgument(1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);

        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(behandlingID), anyCollection());
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(false));
    }

    private Instant månederOgDagerSiden(long mnd, long dager) {
        return LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
