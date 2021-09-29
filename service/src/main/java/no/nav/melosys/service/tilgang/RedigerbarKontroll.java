package no.nav.melosys.service.tilgang;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.stereotype.Service;

@Service
class RedigerbarKontroll {

    private static final Map<Ressurs, Predicate<Behandlingsresultat>> RESSURS_REDIGERBAR_MAP = Map.of(
        Ressurs.AVKLARTE_FAKTA, b -> !b.erArtikkel16MedSendtAnmodningOmUnntak(),
        Ressurs.VILKÅR, b -> !b.erArtikkel16MedSendtAnmodningOmUnntak()
    );

    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    RedigerbarKontroll(BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    public void sjekkTilordnetSaksbehandlerOgRedigerbar(Behandling behandling, Ressurs ressurs, String saksbehandler) {
        final var saksnummer = behandling.getFagsak().getSaksnummer();

        if (!oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)) {
            throw new FunksjonellException(
                "Forsøk på å endre behandling med id %s som er ikke-redigerbar eller ikke er tilordnet %s".formatted(behandling.getId(), saksbehandler)
            );
        }

        sjekkRessursRedigerbar(behandling, ressurs);
    }

    public void sjekkRessursRedigerbar(Behandling behandling, Ressurs ressurs) {

        Optional.ofNullable(RESSURS_REDIGERBAR_MAP.get(ressurs))
            .ifPresent(redigerbarSjekk -> {
                var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
                if (!redigerbarSjekk.test(behandlingsresultat)) {
                    throw new FunksjonellException("Kan ikke endre %s for behandling %s".formatted(ressurs, behandling.getId()));
                }
            });

        validerBehandlingRedigerbar(behandling);
    }

    private void validerBehandlingRedigerbar(Behandling behandling) {
        if(!behandling.erRedigerbar()) {
            throw new FunksjonellException("Forsøk på å endre en ikke-redigerbar behandling med id %s".formatted(behandling.getId()));
        }
    }
}
