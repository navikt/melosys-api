package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;

public class LagreMedfolgendeFamilieDto {
    private final Set<MedfolgendeFamilieDto> medfolgendeFamilie;

    public LagreMedfolgendeFamilieDto(Set<MedfolgendeFamilieDto> medfolgendeFamilie) {
        this.medfolgendeFamilie = medfolgendeFamilie;
    }

    public Set<MedfolgendeFamilieDto> getMedfolgendeFamilie() {
        return medfolgendeFamilie;
    }

    public AvklarteMedfolgendeFamilie til(){
        return new AvklarteMedfolgendeFamilie(
            medfolgendeFamilie.stream()
                .filter(MedfolgendeFamilieDto::isOmfattet)
                .map(familieDto -> new OmfattetFamilie(familieDto.getUuid())).collect(Collectors.toSet()),
            medfolgendeFamilie.stream()
                .filter(MedfolgendeFamilieDto::erIkkeOmfattet)
                .map(familieDto -> new IkkeOmfattetFamilie(familieDto.getUuid(), familieDto.getBegrunnelseKode(), familieDto.getBegrunnelseFritekst())).collect(Collectors.toSet()));
    }
}
