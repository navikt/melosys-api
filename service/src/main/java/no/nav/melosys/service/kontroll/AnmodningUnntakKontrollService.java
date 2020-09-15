package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AnmodningUnntakKontrollService extends AdresseUtlandKontroller {

    private final BehandlingService behandlingService;

    public AnmodningUnntakKontrollService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID) throws FunksjonellException {
        return utførKontroller(
            behandlingService.hentBehandling(behandlingID),
            anmodningUnntakKontroller()
        );
    }

    private static Set<Function<BehandlingsgrunnlagData, Kontrollfeil>> anmodningUnntakKontroller() {
        return Set.of(
            AdresseUtlandKontroller::arbeidsstedManglerFelter,
            AdresseUtlandKontroller::foretakUtlandManglerFelter
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Set<Function<BehandlingsgrunnlagData, Kontrollfeil>> kontroller
    ) {
        BehandlingsgrunnlagData behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        return kontroller.stream()
            .map(f -> f.apply(behandlingsgrunnlagData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
