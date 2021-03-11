package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;

import java.time.LocalDate;
import java.util.List;

public class RepresentantDataDto {
    private final String nummer;
    private final String navn;
    private final List<String> adresselinjer;
    private final String postnummer;
    private final String orgnr;

    public RepresentantDataDto(String nummer, String navn, List<String> adresselinjer, String postnummer, String orgnr) {
        this.nummer = nummer;
        this.navn = navn;
        this.adresselinjer = adresselinjer;
        this.postnummer = postnummer;
        this.orgnr = orgnr;
    }

    public String getNummer() {
        return nummer;
    }

    public String getNavn() {
        return navn;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnummer() {
        return postnummer;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public static RepresentantDataDto av(AvgiftOverforingRepresentantDataDto avgiftOverforingRepresentantDataDto) {
        return new RepresentantDataDto(
            avgiftOverforingRepresentantDataDto.getId(),
            avgiftOverforingRepresentantDataDto.getNavn(),
            avgiftOverforingRepresentantDataDto.getAdresselinjer(),
            avgiftOverforingRepresentantDataDto.getPostnummer(),
            avgiftOverforingRepresentantDataDto.getOrgnr());
    }
}
