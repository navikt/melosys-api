package no.nav.melosys.service.brev.components;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentMuligeProduserbaredokumenterComponentTest {

    @Mock
    private BehandlingService behandlingService;

    @InjectMocks
    private HentMuligeProduserbaredokumenterComponent hentMuligeProduserbaredokumenterComponent;

    private final Behandling behandling = lagBehandling();

    @Test
    void hentBrevMaler_tilBruker_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(123L, BRUKER);


        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_tilArbeidsgiver_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(123L, ARBEIDSGIVER);


        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_ARBEIDSGIVER,
                GENERELT_FRITEKSTBREV_ARBEIDSGIVER
            );
    }

    @Test
    void hentBrevMaler_tilVirksomhet_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(123L, VIRKSOMHET);


        assertThat(brevMaler).hasSize(1).containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentBrevMaler_behandlingAvsluttet_returnererTomListe() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevMaler).isEmpty();
    }

    @Test
    void hentBrevMaler_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(321L, BRUKER);


        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_behandlingErKlage_returnererKorrekt() {
        behandling.setType(Behandlingstyper.KLAGE);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);


        List<Produserbaredokumenter> brevMaler = hentMuligeProduserbaredokumenterComponent.hentMuligeProduserbaredokumenter(123L, BRUKER);


        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        return fagsak;
    }
}
