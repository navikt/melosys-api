package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedigerbarKontrollTest {

    private Behandling behandling;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private RedigerbarKontroll redigerbarKontroll;

    @BeforeEach
    void setup() {
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(11111L)
            .medFagsak(FagsakTestFactory.lagFagsak())
            .build();
        redigerbarKontroll = new RedigerbarKontroll(behandlingsresultatService);
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_ukjentRessursBehandlingIkkeRedigerbar_kasterFeil() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT))
            .withMessageContaining("ikke-redigerbar");
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_behandlingRedigerbar_kasterIkkeFeil() {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThatNoException()
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT));
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaIkkeSendtAnmodningOmUnntak_kasterIkkeFeil() {
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        assertThatNoException()
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.AVKLARTE_FAKTA));
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaErSendtAnmodningOmUnntak_kasterFeil() {
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.AVKLARTE_FAKTA))
            .withMessageContaining("Kan ikke endre");
    }
}
