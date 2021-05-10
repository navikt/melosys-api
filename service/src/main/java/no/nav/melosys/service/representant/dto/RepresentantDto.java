package no.nav.melosys.service.representant.dto;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;

public record RepresentantDto(String nummer, String navn) {

    public static RepresentantDto av(AvgiftOverforingRepresentantDto avgiftOverforingRepresentantDto) {
        return new RepresentantDto(avgiftOverforingRepresentantDto.getId(), avgiftOverforingRepresentantDto.getNavn());
    }
}
