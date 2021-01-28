package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;

public class MedfolgendeFamilieDto {
    private final String uuid;
    private final boolean omfattet;
    private final String begrunnelseKode;
    private final String begrunnelseFritekst;

    public MedfolgendeFamilieDto(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.omfattet = omfattet;
        this.begrunnelseKode = begrunnelseKode;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

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

    public String getUuid() {
        return uuid;
    }

    public boolean isOmfattet() {
        return omfattet;
    }

    public boolean isIkkeOmfattet() {
        return !isOmfattet();
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }
}
