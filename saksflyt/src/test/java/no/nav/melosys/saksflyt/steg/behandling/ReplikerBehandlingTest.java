package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplikerBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private SaksbehandlingRegler behandlingReplikeringsRegler;
    private ReplikerBehandling replikerBehandling;

    private final Fagsak fagsak = new Fagsak();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        replikerBehandling = new ReplikerBehandling(fagsakService, behandlingService, behandlingReplikeringsRegler);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "MelTest-1");
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        when(fagsakService.hentFagsak("MelTest-1")).thenReturn(fagsak);
    }


    @Test
    void utfør_behandlingSomErUtgangspunktetForVurderingErAktiv_kasterFeil() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        replikertBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.now());
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Støtter ikke opprettelse av ny behandling når behandling som er utgangspunkt for revurdering er aktiv");
    }

    @Test
    void utfør_finnesBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        replikertBehandling.setTema(Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND);
        replikertBehandling.setType(Behandlingstyper.NY_VURDERING);
        replikertBehandling.setFagsak(fagsak);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.now());
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void utfør_finnesIkkeBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsak.setBehandlinger(List.of(behandling));
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        replikertBehandling.setTema(Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND);
        replikertBehandling.setType(Behandlingstyper.NY_VURDERING);
        replikertBehandling.setFagsak(fagsak);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.now());
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void utfør_finnesIkkeBehandlingSomErUtgangspunktForRevurdering_kasterFeil() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsak.setBehandlinger(List.of(behandling));
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.now());
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Finner ikke behandling som kan replikeres. Denne fantes ved opprettelse av prosessen");
    }

    @Test
    void utfør_behandlingsårsakErIkkeSatt_kasterFeil() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.now());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Mangler mottaksdato eller behandlingsårsaktype");
    }

    @Test
    void utfør_mottaksdatoErIkkeSatt_kasterFeil() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Mangler mottaksdato eller behandlingsårsaktype");
    }
}
