package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentMuligeProduserbaredokumenterServiceTest {

    @Mock
    private BehandlingService behandlingService;

    @InjectMocks
    private HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        behandling = lagBehandling();
    }

    @Test
    void hentMuligeProduserbaredokumenter_tilBruker_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(123L, BRUKER);


        assertThat(brevmaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentMuligeProduserbaredokumenter_tilArbeidsgiver_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(123L, ARBEIDSGIVER);


        assertThat(brevmaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_ARBEIDSGIVER,
                GENERELT_FRITEKSTBREV_ARBEIDSGIVER
            );
    }

    @Test
    void hentMuligeProduserbaredokumenter_tilVirksomhet_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(123L, VIRKSOMHET);


        assertThat(brevmaler).hasSize(1).containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentMuligeProduserbaredokumenter_behandlingAvsluttet_returnererTomListe() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevmaler).isEmpty();
    }

    @Test
    void hentMuligeProduserbaredokumenter_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevmaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentMuligeProduserbaredokumenter_behandlingErNyVurderingMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevmaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentMuligeProduserbaredokumenter_bruker_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevmaler)
            .hasSize(1)
            .containsExactlyInAnyOrder(GENERELT_FRITEKSTBREV_BRUKER);
    }

    @Test
    void hentMuligeProduserbaredokumenter_arbeidsgiver_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, ARBEIDSGIVER);


        assertThat(brevmaler)
            .hasSize(1)
            .containsExactlyInAnyOrder(GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
    }

    @Test
    void hentMuligeProduserbaredokumenter_annenOrganisasjon_behandlingErManglendeInnbetalingTrygdavgift_returnererKunFritekstbrev() {
        behandling.setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        behandling.getFagsak().getAktører().add(bruker);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, ANNEN_ORGANISASJON);


        assertThat(brevmaler)
            .hasSize(1)
            .containsExactlyInAnyOrder(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentMuligeProduserbaredokumenter_annenOrganisasjon_virksomhetErHovedpart_returnererKunFritekstbrev() {
        var virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        behandling.getFagsak().getAktører().add(virksomhet);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(321L, ANNEN_ORGANISASJON);


        assertThat(brevmaler)
            .hasSize(1)
            .containsExactlyInAnyOrder(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentMuligeProduserbaredokumenter_behandlingErKlage_returnererKorrekt() {
        behandling.setType(Behandlingstyper.KLAGE);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        var brevmaler = hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(123L, BRUKER);


        assertThat(brevmaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        return behandling;
    }
}
