package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

public record MedfolgendeFamilieDto(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
    public static Set<MedfolgendeFamilieDto> av(Set<AvklartefaktaDto> avklartefaktas) {
        return avklartefaktas.stream()
            .filter(MedfolgendeFamilieDto::erMedfolgendeFamilieFakta)
            .map(avklartefakta -> new MedfolgendeFamilieDto(
                avklartefakta.getSubjektID(),
                tilBoolean(avklartefakta.getFakta()),
                avklartefakta.getBegrunnelseKoder().stream().findFirst().orElse(null),
                avklartefakta.getBegrunnelseFritekst()))
            .collect(Collectors.toSet());
    }

    private static boolean erMedfolgendeFamilieFakta(AvklartefaktaDto avklartfakta) {
        return VURDERING_LOVVALG_BARN.getKode().equals(avklartfakta.getReferanse()) ||
            VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode().equals(avklartfakta.getReferanse());
    }

    private static boolean tilBoolean(List<String> fakta) {
        return VALGT_FAKTA.equals(fakta.get(0));
    }

    public boolean erIkkeOmfattet() {
        return !omfattet;
    }
}
