package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;

public class RepresentantDto {
    private final String nummer;
    private final String navn;

    public RepresentantDto(String nummer, String navn) {
        this.nummer = nummer;
        this.navn = navn;
    }

    public String getNummer() {
        return nummer;
    }

    public String getNavn() {
        return navn;
    }

    public static RepresentantDto av(AvgiftOverforingRepresentantDto avgiftOverforingRepresentantDto) {
        return new RepresentantDto(
            avgiftOverforingRepresentantDto.getId(),
            avgiftOverforingRepresentantDto.getNavn()
        );
    }
}
