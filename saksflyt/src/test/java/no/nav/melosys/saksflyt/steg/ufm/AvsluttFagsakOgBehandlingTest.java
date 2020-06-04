package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    @Before
    public void setup() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(behandlingsresultatService, behandlingService, fagsakService);
    }

    @Test
    public void utfør_ikkeArt13_forventFagsakAvsluttes() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        when(fagsakService.hentFagsak(eq(fagsak.getSaksnummer()))).thenReturn(fagsak);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(eq(behandling.getFagsak()), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(behandlingsresultatService).oppdaterUtfallRegistreringUnntak(eq(behandling.getId()), eq(Utfallregistreringunntak.GODKJENT));
        verify(behandlingService, never()).oppdaterStatus(anyLong(), any(Behandlingsstatus.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    @Test
    public void utfør_art13_forventBehandlingsstatusOppdateres() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        when(fagsakService.hentFagsak(eq(fagsak.getSaksnummer()))).thenReturn(fagsak);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService, never()).avsluttFagsakOgBehandling(any(Fagsak.class), any(Saksstatuser.class));
        verify(behandlingsresultatService).oppdaterUtfallRegistreringUnntak(eq(behandling.getId()), eq(Utfallregistreringunntak.GODKJENT));
        verify(behandlingService).oppdaterStatus(eq(1L), eq(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}