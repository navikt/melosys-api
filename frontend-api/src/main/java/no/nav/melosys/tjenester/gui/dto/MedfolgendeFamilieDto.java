package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;

public record MedfolgendeFamilieDto(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
    public static Set<MedfolgendeFamilieDto> av(Set<AvklartefaktaDto> avklartefaktas) {
        return avklartefaktas.stream()
            .map(avklartefakta -> new MedfolgendeFamilieDto(
                avklartefakta.getSubjektID(),
                tilBoolean(avklartefakta.getFakta()),
                avklartefakta.getBegrunnelseKoder().stream().findFirst().orElse(null),
                avklartefakta.getBegrunnelseFritekst()))
            .collect(Collectors.toSet());
    }

    private static boolean tilBoolean(List<String> fakta) {
        return VALGT_FAKTA.equals(fakta.get(0));
    }

    public boolean erIkkeOmfattet() {
        return !omfattet;
    }
}
