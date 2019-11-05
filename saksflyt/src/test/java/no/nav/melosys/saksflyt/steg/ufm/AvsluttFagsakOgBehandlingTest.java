package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private FagsakService fagsakService;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() throws Exception {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(behandlingsresultatRepository, fagsakService);
        when(behandlingsresultatRepository.findById(any())).thenReturn(Optional.of(behandlingsresultat));
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

        verify(fagsakService).avsluttFagsakOgBehandling(eq(behandling.getFagsak()), eq(Saksstatuser.LOVVALG_AVKLART), eq(behandling));
        verify(behandlingsresultatRepository).findById(1L);
        verify(behandlingsresultatRepository).save(any(Behandlingsresultat.class));

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        assertThat(behandlingsresultat.getUtfallRegistreringUnntak()).isEqualTo(Utfallregistreringunntak.GODKJENT);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}