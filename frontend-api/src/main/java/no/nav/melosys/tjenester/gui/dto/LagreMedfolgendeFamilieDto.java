package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;

public record LagreMedfolgendeFamilieDto(Set<MedfolgendeFamilieDto> medfolgendeFamilie) {
    public AvklarteMedfolgendeFamilie til(){
        return new AvklarteMedfolgendeFamilie(
            medfolgendeFamilie.stream()
                .filter(MedfolgendeFamilieDto::omfattet)
                .map(familieDto -> new OmfattetFamilie(familieDto.uuid())).collect(Collectors.toSet()),
            medfolgendeFamilie.stream()
                .filter(MedfolgendeFamilieDto::erIkkeOmfattet)
                .map(familieDto -> new IkkeOmfattetFamilie(familieDto.uuid(), familieDto.begrunnelseKode(), familieDto.begrunnelseFritekst())).collect(Collectors.toSet()));
    }
}
