package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;

import java.time.LocalDate;
import java.util.List;

public class RepresentantDataDto {
    private final String nummer;
    private final String navn;
    private final List<String> adresselinjer;
    private final String postnummer;
    private final String telefon;
    private final String orgnr;
    private final String endretAv;
    private final LocalDate endretDato;

    public RepresentantDataDto(String nummer, String navn, List<String> adresselinjer, String postnummer, String telefon, String orgnr, String endretAv, LocalDate endretDato) {
        this.nummer = nummer;
        this.navn = navn;
        this.adresselinjer = adresselinjer;
        this.postnummer = postnummer;
        this.telefon = telefon;
        this.orgnr = orgnr;
        this.endretAv = endretAv;
        this.endretDato = endretDato;
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

    public String getTelefon() {
        return telefon;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDate getEndretDato() {
        return endretDato;
    }

    public static RepresentantDataDto av(AvgiftOverforingRepresentantDataDto avgiftOverforingRepresentantDataDto) {
        return new RepresentantDataDto(
            avgiftOverforingRepresentantDataDto.getId(),
            avgiftOverforingRepresentantDataDto.getNavn(),
            avgiftOverforingRepresentantDataDto.getAdresselinjer(),
            avgiftOverforingRepresentantDataDto.getPostnummer(),
            avgiftOverforingRepresentantDataDto.getTelefon(),
            avgiftOverforingRepresentantDataDto.getOrgnr(),
            avgiftOverforingRepresentantDataDto.getEndretAv(),
            avgiftOverforingRepresentantDataDto.getEndretDato());
    }
}
