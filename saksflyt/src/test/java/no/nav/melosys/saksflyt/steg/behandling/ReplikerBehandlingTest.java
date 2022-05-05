package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplikerBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private ReplikerBehandling replikerBehandling;

    private final Fagsak fagsak = new Fagsak();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        replikerBehandling = new ReplikerBehandling(fagsakService, behandlingService);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "MelTest-1");
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        when(fagsakService.hentFagsak("MelTest-1")).thenReturn(fagsak);
    }


    @Test
    void utfør_behandlingSomErUtgangspunktetForVurderingErAktiv_kasterFeil() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));
        when(fagsakService.hentBehandlingSomErUtgangspunktForRevurdering(fagsak)).thenReturn(Optional.empty());
        when(behandlingService.replikerBehandlingUtenBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(new Behandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Støtter ikke opprettelse av ny behandling når behandling som er utgangspunkt for revurdering er aktiv");
    }

    @Test
    void utfør_finnesBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        when(fagsakService.hentBehandlingSomErUtgangspunktForRevurdering(fagsak)).thenReturn(Optional.of(behandling));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void utfør_finnesIkkeBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        fagsak.setBehandlinger(List.of(behandling));
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        when(fagsakService.hentBehandlingSomErUtgangspunktForRevurdering(fagsak)).thenReturn(Optional.empty());
        when(behandlingService.replikerBehandlingUtenBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingUtenBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }
}
