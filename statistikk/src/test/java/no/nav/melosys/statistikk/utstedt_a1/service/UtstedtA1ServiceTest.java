package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtstedtA1ServiceTest {
    @Mock
    private UtstedtA1Producer utstedtA1Producer;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private UtstedtA1AivenProducer utstedtA1AivenProducer;
    private FakeUnleash unleash = new FakeUnleash();

    @Captor
    private ArgumentCaptor<UtstedtA1Melding> captor;

    private UtstedtA1Service utstedtA1Service;

    private static final Long BEHANDLING_ID = 123L;

    @BeforeEach
    void setUp() {
        utstedtA1Service = new UtstedtA1Service(utstedtA1Producer, utstedtA1AivenProducer, behandlingsresultatService, landvelgerService, unleash);
    }

    @Test
    void sendMeldingOmUtstedtA1() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat());
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Landkoder.SE));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService).hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID);
        verify(utstedtA1Producer).produserMelding(captor.capture());

        UtstedtA1Melding melding = captor.getValue();
        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isEqualTo("SE");
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_12_1);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    @Test
    void sendMeldingOmUtstedtA1PåAiven() {
        unleash.enable("melosys.api.producer-aiven");

        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat());
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Landkoder.SE));
        when(utstedtA1AivenProducer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService).hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID);
        verify(utstedtA1AivenProducer).produserMelding(captor.capture());

        UtstedtA1Melding melding = captor.getValue();
        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isEqualTo("SE");
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_12_1);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    @Test
    void sendMeldingOmUtstedtA1_avslag_forventIngenMelding() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat(true, lagBehandling()));

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService, never()).hentUtenlandskTrygdemyndighetsland(anyLong());
        verify(utstedtA1Producer, never()).produserMelding(any(UtstedtA1Melding.class));
    }

    @Test
    void sendMeldingOmUtstedtA1_art13_forventTomLandkode() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat(false, lagBehandling(), Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService, never()).hentUtenlandskTrygdemyndighetsland(anyLong());
        verify(utstedtA1Producer).produserMelding(captor.capture());

        UtstedtA1Melding melding = captor.getValue();
        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isNull();
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_13_1);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    @Test
    void sendMeldingOmUtstedtA1_art11_forventTomLandkode() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat(false, lagBehandling(), Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService, never()).hentUtenlandskTrygdemyndighetsland(anyLong());
        verify(utstedtA1Producer).produserMelding(captor.capture());

        UtstedtA1Melding melding = captor.getValue();
        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isNull();
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_11_3_a);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    @Test
    void sendMeldingOmUtstedtA1_art12MedTilleggsbestemmelseArt11_forventLandkode() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(false, lagBehandling(), Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().iterator().next().setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Landkoder.SE));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingsresultatService).hentBehandlingsresultat(BEHANDLING_ID);
        verify(landvelgerService).hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID);
        verify(utstedtA1Producer).produserMelding(captor.capture());

        UtstedtA1Melding melding = captor.getValue();
        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isEqualTo("SE");
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_12_1);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    private static Behandling lagBehandling() {
        return lagBehandling(Behandlingsstatus.AVSLUTTET);
    }

    private static Behandling lagBehandling(Behandlingsstatus behandlingsstatus) {
        Aktoer bruker = new Aktoer();
        bruker.setAktørId("1234567891234");
        bruker.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setAktører(Set.of(bruker));

        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(fagsak);
        behandling.setStatus(behandlingsstatus);

        return behandling;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        return lagBehandlingsresultat(false, lagBehandling());
    }

    private static Behandlingsresultat lagBehandlingsresultat(boolean erAvslag, Behandling behandling) {
        return lagBehandlingsresultat(erAvslag, behandling, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
    }

    private static Behandlingsresultat lagBehandlingsresultat(boolean erAvslag, Behandling behandling, LovvalgBestemmelse bestemmelse) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(bestemmelse);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusMonths(3));

        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(Instant.now());
        vedtakMetadata.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        behandlingsresultat.setType(erAvslag
            ? Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
            : Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);

        return behandlingsresultat;
    }
}
