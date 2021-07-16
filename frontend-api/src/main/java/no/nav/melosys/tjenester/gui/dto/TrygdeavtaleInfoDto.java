package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.service.kodeverk.KodeDto;

public record TrygdeavtaleInfoDto(String aktoerId, String behandlingstema,
                                  List<OrgIdNavnDto> virksomheter, List<KodeDto> barn,
                                  KodeDto ektefelleSamboer) {

    public TrygdeavtaleInfoDto(String aktoerId, String behandlingstema, List<OrgIdNavnDto> virksomheter, List<MedfolgendeFamilie> familie) {
        this(
            aktoerId,
            behandlingstema,
            virksomheter,
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erBarn),
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erEktefelleSamboer)
                .stream().findFirst().orElse(null)
        );
    }

    public static List<KodeDto> filtrerOgMapFamilie(List<MedfolgendeFamilie> familie, Predicate<MedfolgendeFamilie> filterfunksjon) {
        return familie.stream()
            .filter(filterfunksjon)
            .map(fm -> new KodeDto(fm.uuid, fm.navn))
            .collect(Collectors.toList());
    }
}
