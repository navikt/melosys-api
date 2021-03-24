package no.nav.melosys.service.representant.dto;

import java.util.List;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;

public record RepresentantDataDto(String nummer,
                                  String navn,
                                  List<String> adresselinjer,
                                  String postnummer,
                                  String orgnr) {

    public static RepresentantDataDto av(AvgiftOverforingRepresentantDataDto avgiftOverforingRepresentantDataDto) {
        return new RepresentantDataDto(
            avgiftOverforingRepresentantDataDto.getId(),
            avgiftOverforingRepresentantDataDto.getNavn(),
            avgiftOverforingRepresentantDataDto.getAdresselinjer(),
            avgiftOverforingRepresentantDataDto.getPostnummer(),
            avgiftOverforingRepresentantDataDto.getOrgnr());
    }
}
