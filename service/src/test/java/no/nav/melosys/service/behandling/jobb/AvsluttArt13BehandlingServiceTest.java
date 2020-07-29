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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = new Behandling();
    private final Fagsak fagsak = new Fagsak();
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final VedtakMetadata vedtakMetadata = new VedtakMetadata();

    private final long behandlingID = 11L;

    @Before
    public void setup() throws IkkeFunnetException {
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
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID)))
            .thenReturn(behandlingsresultat);
    }

    @Test
    public void avsluttBehandlingArt13_ikkeArt13_kasterException() throws FunksjonellException, TekniskException {
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));
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
    public void avsluttBehandlingArt13_søknad2Mnd1DagSidenEndretDato_medlOppdatertOgBehandlingBlirAvsluttet()
        throws FunksjonellException, TekniskException {
        behandlingsresultat.setEndretDato(månederOgDagerSiden(2, 1));
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    public void avsluttBehandlingArt13_søknad3MndSidenEndretDatoUtpekingUtenVedtak_lovvalgsperiodeOpprettetOgBehandlingAvsluttet()
        throws FunksjonellException, TekniskException {

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