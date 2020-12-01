package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtstedtA1ServiceTest {
    @Mock
    private UtstedtA1Producer utstedtA1Producer;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private UtstedtA1Service utstedtA1Service;

    private static final Long BEHANDLING_ID = 123L;

    @BeforeEach
    void setUp() {
        utstedtA1Service = new UtstedtA1Service(utstedtA1Producer, behandlingService, behandlingsresultatService, landvelgerService, applicationEventPublisher);
    }

    @Test
    void sendMeldingOmUtstedtA1() throws Exception {
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(lagBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(lagBehandlingsresultat());
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(BEHANDLING_ID))).thenReturn(List.of(Landkoder.SE));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        UtstedtA1Melding melding = utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID));
        verify(behandlingsresultatService).hentBehandlingsresultat(eq(BEHANDLING_ID));
        verify(landvelgerService).hentUtenlandskTrygdemyndighetsland(eq(BEHANDLING_ID));
        verify(utstedtA1Producer).produserMelding(any(UtstedtA1Melding.class));

        assertThat(melding).isNotNull();
        assertThat(melding.getSerienummer()).isEqualTo("MEL-123123");
        assertThat(melding.getUtsendtTilLand()).isEqualTo("SE");
        assertThat(melding.getArtikkel()).isEqualTo(Lovvalgsbestemmelse.ART_12_1);
        assertThat(melding.getTypeUtstedelse()).isEqualTo(A1TypeUtstedelse.FØRSTEGANG);
    }

    @Test
    void sendMeldingOmUtstedtA1_avslag_forventIngenMelding() throws Exception {
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(lagBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(lagBehandlingsresultat(true));

        UtstedtA1Melding melding = utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID));
        verify(behandlingsresultatService).hentBehandlingsresultat(eq(BEHANDLING_ID));
        verify(landvelgerService, never()).hentUtenlandskTrygdemyndighetsland(anyLong());
        verify(utstedtA1Producer, never()).produserMelding(any(UtstedtA1Melding.class));

        assertThat(melding).isNull();
    }

    @Test
    void sendMeldingOmUtstedtA1_gittBehandlingOgBehandlingsresultat_forventIngenKallMotServicer() throws Exception {
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(BEHANDLING_ID))).thenReturn(List.of(Landkoder.SE));
        when(utstedtA1Producer.produserMelding(any(UtstedtA1Melding.class))).thenAnswer(returnsFirstArg());

        UtstedtA1Melding melding = utstedtA1Service.sendMeldingOmUtstedtA1(lagBehandling(), lagBehandlingsresultat());

        verify(behandlingService, never()).hentBehandlingUtenSaksopplysninger(anyLong());
        verify(behandlingsresultatService, never()).hentBehandlingsresultat(anyLong());
        verify(landvelgerService).hentUtenlandskTrygdemyndighetsland(eq(BEHANDLING_ID));
        verify(utstedtA1Producer).produserMelding(any(UtstedtA1Melding.class));

        assertThat(melding).isNotNull();
    }

    private static Behandling lagBehandling() {
        Aktoer bruker = new Aktoer();
        bruker.setAktørId("1234567891234");
        bruker.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setAktører(Set.of(bruker));

        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(fagsak);

        return behandling;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        return lagBehandlingsresultat(false);
    }

    private static Behandlingsresultat lagBehandlingsresultat(boolean erAvslag) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusMonths(3));

        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(Instant.now());
        vedtakMetadata.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        behandlingsresultat.setType(erAvslag
            ? Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
            : Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);

        return behandlingsresultat;
    }
}