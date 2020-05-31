package no.nav.melosys.service.behandling.jobb;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttArt13BehandlingServiceTest {

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Collection<Lovvalgsperiode>> lovvalgsperiodeCaptor;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private Behandling behandling = new Behandling();
    private Fagsak fagsak = new Fagsak();
    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private VedtakMetadata vedtakMetadata = new VedtakMetadata();

    private final long behandlingID = 11L;

    @Before
    public void setup() throws IkkeFunnetException {
        avsluttArt13BehandlingService = new AvsluttArt13BehandlingService(behandlingService, fagsakService, behandlingsresultatService, medlPeriodeService, lovvalgsperiodeService);

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
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID)))
            .thenReturn(behandlingsresultat);
    }

    @Test
    public void avsluttBehandlingArt13_ikkeArt13_kasterException() throws FunksjonellException, TekniskException {
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2,1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);


        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse");

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
    }

    @Test
    public void avsluttBehandlingArt13_søknad1MndSidenVedtak_behandlingIkkeAvlsuttet() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(1, 0));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService, never()).avsluttFagsakOgBehandling(any(), any());
    }

    @Test
    public void avsluttBehandlingArt13_norgeUtpekt2Mnd1DagSidenVedtak_behandlingBlirAvlsuttet() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(true));
    }

    @Test
    public void avsluttBehandlingArt13_norgeUtpektVedtakIkkeLagret_kasterException() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandlingsresultat.setVedtakMetadata(null);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("har ikke et vedtak og status kan da ikke settes til AVSLUTTET");
        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
    }

    @Test
    public void avsluttBehandlingArt13_annetLandUtpekt2Mnd1DagSidenEndretDato_behandlingBlirAvsluttet() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        behandlingsresultat.setEndretDato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(true));
    }


    @Test
    public void avsluttBehandlingArt13_søknad2Mnd1DagSidenEndretDato_lovvalgsperiodeOpprettBehandlingBlirAvsluttet() throws FunksjonellException, TekniskException {
        behandlingsresultat.setEndretDato(månederOgDagerSiden(2, 1));
        behandlingsresultat.getLovvalgsperioder().clear();
        behandlingsresultat.setVedtakMetadata(null);

        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(LocalDate.now(), LocalDate.now().plusYears(1), Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, null);
        utpekingsperiode.setMedlPeriodeID(123L);
        utpekingsperiode.setSendtUtland(LocalDate.now());
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        when(lovvalgsperiodeService.lagreLovvalgsperioder(eq(behandlingID), anyCollection()))
            .thenReturn(Collections.singletonList(lovvalgsperiode));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(behandlingID), lovvalgsperiodeCaptor.capture());
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));

        Collection<Lovvalgsperiode> lagretLovvalgsperioder = lovvalgsperiodeCaptor.getValue();
        assertThat(lagretLovvalgsperioder).isNotEmpty().hasSize(1);

        Lovvalgsperiode lovvalgsperiode = lagretLovvalgsperioder.iterator().next();
        assertThat(lovvalgsperiode.getBestemmelse()).isEqualTo(utpekingsperiode.getBestemmelse());
        assertThat(lovvalgsperiode.getFom()).isEqualTo(utpekingsperiode.getFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(utpekingsperiode.getTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
        assertThat(lovvalgsperiode.getDekning()).isEqualTo(Trygdedekninger.UTEN_DEKNING);
        assertThat(lovvalgsperiode.getMedlPeriodeID()).isEqualTo(utpekingsperiode.getMedlPeriodeID()).isNotNull();
        assertThat(lovvalgsperiode.getTilleggsbestemmelse()).isEqualTo(utpekingsperiode.getTilleggsbestemmelse());
    }

    private Instant månederOgDagerSiden(long mnd, long dager) {
        return LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}