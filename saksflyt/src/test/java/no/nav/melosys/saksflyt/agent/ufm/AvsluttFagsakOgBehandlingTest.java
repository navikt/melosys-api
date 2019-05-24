package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.UtfallRegistreringUnntak;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
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
    private OppdaterFagsakOgBehandling felles;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(felles, behandlingsresultatRepository);
        when(behandlingsresultatRepository.findById(any())).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void utfør() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(felles).oppdaterFagsakOgBehandlingStatuser(eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART), eq(Behandlingsstatus.AVSLUTTET));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        assertThat(behandlingsresultat.getUtfallRegistreringUnntak()).isEqualTo(UtfallRegistreringUnntak.GODKJENT);
    }

}