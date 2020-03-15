package no.nav.melosys.service.jobb;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttArt13BehandlingTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlFasade medlFasade;

    private AvsluttArt13BehandlingJobb avsluttArt13BehandlingJobb;
    private AvsluttArt13BehandlingService avsluttArt13BehandlingService;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private Behandling behandling = new Behandling();
    private Fagsak fagsak = new Fagsak();
    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private VedtakMetadata vedtakMetadata = new VedtakMetadata();
    private final Long behandlingID = 11L;
    private final String saksnummer = "MEL-11";

    @Before
    public void setup() throws IkkeFunnetException {
        avsluttArt13BehandlingService = spy(new AvsluttArt13BehandlingService(behandlingService, fagsakService, behandlingsresultatService, medlFasade));
        avsluttArt13BehandlingJobb = new AvsluttArt13BehandlingJobb(behandlingService, behandlingsresultatService, avsluttArt13BehandlingService);

        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        behandling.setFagsak(fagsak);
        fagsak.setSaksnummer(saksnummer);
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A);
        lovvalgsperiode.setMedlPeriodeID(123L);

        when(behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING))
            .thenReturn(Collections.singleton(behandling));
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID)))
            .thenReturn(behandlingsresultat);
    }

    @Test
    public void avsluttBehandlingArt13_ikkeArt13_behandlingIkkeAvlsuttet() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        avsluttArt13BehandlingJobb.avsluttBehandlingArt13();

        verify(avsluttArt13BehandlingService, never()).avsluttBehandling(anyLong());
    }

    @Test
    public void avsluttBehandlingArt13_søknad1MndSidenVedtak_behandlingIkkeAvlsuttet() throws FunksjonellException, TekniskException {
        behandling.setType(Behandlingstyper.SOEKNAD);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(1, 0));

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13();
        verify(avsluttArt13BehandlingService, never()).avsluttBehandling(anyLong());
    }

    @Test
    public void avsluttBehandlingArt13_norgeUtpekt2Mnd1DagSidenVedtak_behandlingBlirAvlsuttet() throws FunksjonellException, TekniskException {
        behandling.setType(Behandlingstyper.BESLUTNING_LOVVALG_NORGE);
        vedtakMetadata.setVedtaksdato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13();
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlFasade).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.SED));
    }

    @Test
    public void avsluttBehandlingArt13_annetLandUtpekt2Mnd1DagSidenEndretDato_behandlingBlirAvlsuttet() throws FunksjonellException, TekniskException {
        behandling.setType(Behandlingstyper.BESLUTNING_LOVVALG_ANNET_LAND);
        behandlingsresultat.setEndretDato(månederOgDagerSiden(2, 1));

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13();
        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(medlFasade).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.SED));
    }

    private Instant månederOgDagerSiden(long mnd, long dager) {
        return LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}