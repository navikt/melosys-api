package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private FagsakService fagsakService;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(behandlingsresultatService, fagsakService);
    }

    @Test
    public void utfør() throws Exception {

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        when(fagsakService.hentFagsak(eq(fagsak.getSaksnummer()))).thenReturn(fagsak);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(eq(behandling.getFagsak()), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(behandlingsresultatService).oppdaterUtfallRegistreringUnntak(eq(behandling.getId()), eq(Utfallregistreringunntak.GODKJENT));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}