package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.domain.*;
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

    private Fagsak fagsak;
    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() {
        replikerBehandling = new ReplikerBehandling(fagsakService, behandlingService, behandlingReplikeringsRegler);
        prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            .medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            .build();
        fagsak = FagsakTestFactory.lagFagsak();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);
    }


    @Test
    void utfør_behandlingSomErUtgangspunktetForVurderingErAktiv_kasterFeil() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .build();

        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .build();

        fagsak.leggTilBehandling(behandling);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            .medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
            .build();
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Støtter ikke opprettelse av ny behandling når behandling som er utgangspunkt for revurdering er aktiv");
    }

    @Test
    void utfør_finnesBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();
        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .medTema(Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND)
            .medType(Behandlingstyper.NY_VURDERING)
            .medFagsak(fagsak)
            .build();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            .medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
            .build();
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void utfør_finnesIkkeBehandlingSomErUtgangspunktForRevurdering_settStegOpprettOppgave() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();
        fagsak.leggTilBehandling(behandling);
        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .medTema(Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND)
            .medType(Behandlingstyper.NY_VURDERING)
            .medFagsak(fagsak)
            .build();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            .medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
            .build();
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        replikerBehandling.utfør(prosessinstans);

        verify(fagsakService).lagre(fagsak);
        assertThat(prosessinstans.getBehandling()).isEqualTo(replikertBehandling);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void utfør_finnesIkkeBehandlingSomErUtgangspunktForRevurdering_kasterFeil() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();
        fagsak.leggTilBehandling(behandling);
        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .build();
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            .medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
            .build();
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Finner ikke behandling som kan replikeres. Denne fantes ved opprettelse av prosessen");
    }

    @Test
    void utfør_behandlingsårsakErIkkeSatt_kasterFeil() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();
        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .build();
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
            .build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Mangler mottaksdato eller behandlingsårsaktype");
    }

    @Test
    void utfør_mottaksdatoErIkkeSatt_kasterFeil() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();
        Behandling replikertBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .build();
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE)).thenReturn(replikertBehandling);
        when(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak)).thenReturn(behandling);

        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            .build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> replikerBehandling.utfør(prosessinstans))
            .withMessageContaining("Mangler mottaksdato eller behandlingsårsaktype");
    }
}
