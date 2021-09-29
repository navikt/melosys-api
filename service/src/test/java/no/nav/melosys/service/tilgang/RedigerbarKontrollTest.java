package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RedigerbarKontrollTest {

    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    private RedigerbarKontroll redigerbarKontroll;

    @BeforeEach
    void setup() {
        redigerbarKontroll = new RedigerbarKontroll();
        behandlingsresultat.setBehandling(behandling);
    }

    @Test
    void validerBehandlingRedigerbar_behandlingIkkeRedigerbar_kasterFeil() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> redigerbarKontroll.validerBehandlingRedigerbar(behandling))
            .withMessageContaining("ikke-redigerbar");
    }

    @Test
    void validerBehandlingRedigerbar_behandlingRedigerbar_kasterIkkeFeil() {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThatNoException()
            .isThrownBy(() -> redigerbarKontroll.validerBehandlingRedigerbar(behandling));
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaIkkeSendtAnmodningOmUnntak_kasterIkkeFeil() {
        assertThatNoException()
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbarOgTilgang(behandlingsresultat, Ressurs.AVKLARTE_FAKTA));
    }

    @Test
    void sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaErSendtAnmodningOmUnntak_kasterFeil() {
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> redigerbarKontroll.sjekkRessursRedigerbarOgTilgang(behandlingsresultat, Ressurs.AVKLARTE_FAKTA))
            .withMessageContaining("Kan ikke endre");
    }
}
