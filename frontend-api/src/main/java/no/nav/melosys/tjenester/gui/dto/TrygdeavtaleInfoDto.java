package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;

public record TrygdeavtaleInfoDto(String aktoerId, String behandlingstema,
                                  List<IdNavnDto> virksomheter, List<IdNavnDto> barn,
                                  IdNavnDto ektefelleSamboer) {

    public TrygdeavtaleInfoDto(String aktoerId, String behandlingstema, Map<String, String> virksomheter, List<MedfolgendeFamilie> familie) {
        this(
            aktoerId,
            behandlingstema,
            mapVirksomheter(virksomheter),
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erBarn),
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erEktefelleSamboer)
                .stream().findFirst().orElse(null)
        );
    }

    public static List<IdNavnDto> mapVirksomheter(Map<String, String> virksomheter) {
        return virksomheter.entrySet().stream()
            .map(virksomhet -> new IdNavnDto(virksomhet.getKey(), virksomhet.getValue()))
            .toList();
    }

    public static List<IdNavnDto> filtrerOgMapFamilie(List<MedfolgendeFamilie> familie, Predicate<MedfolgendeFamilie> filterfunksjon) {
        return familie.stream()
            .filter(filterfunksjon)
            .map(familiemedlem -> new IdNavnDto(familiemedlem.uuid, familiemedlem.navn))
            .toList();
    }
}
