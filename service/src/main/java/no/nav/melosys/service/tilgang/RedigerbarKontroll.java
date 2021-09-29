package no.nav.melosys.service.tilgang;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.stereotype.Service;

@Service
class RedigerbarKontroll {

    private static final Map<Ressurs, Predicate<Behandlingsresultat>> RESSURS_REDIGERBAR_MAP = Map.of(
        Ressurs.AVKLARTE_FAKTA, b -> !b.erArtikkel16MedSendtAnmodningOmUnntak(),
        Ressurs.VILKÅR, b -> !b.erArtikkel16MedSendtAnmodningOmUnntak()
    );

    void validerBehandlingRedigerbar(Behandling behandling) {
        if(!behandling.erRedigerbar()) {
            throw new FunksjonellException("Forsøk på å endre en ikke-redigerbar behandling med id %s".formatted(behandling.getId()));
        }
    }

    public void sjekkRessursRedigerbarOgTilgang(Behandlingsresultat behandlingsresultat, Ressurs ressurs) {

        Optional.ofNullable(RESSURS_REDIGERBAR_MAP.get(ressurs))
            .ifPresent(redigerbarSjekk -> {
                if (!redigerbarSjekk.test(behandlingsresultat)) {
                    throw new FunksjonellException("Kan ikke endre %s for behandling %s".formatted(ressurs, behandlingsresultat.getId()));
                }
            });

        validerBehandlingRedigerbar(behandlingsresultat.getBehandling());
    }
}
