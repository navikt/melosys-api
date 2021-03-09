package no.nav.melosys.integrasjon.avgiftoverforing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class AvgiftOverforingRepresentantDataDto {
    private final String id;
    private final String navn;
    private final List<String> adresselinjer;
    private final String postnummer;
    private final String telefon;
    private final String orgnr;
    private final String endretAv;
    private final LocalDate endretDato;

    @JsonCreator
    public AvgiftOverforingRepresentantDataDto(@JsonProperty("id") String id,
                                               @JsonProperty("navn") String navn,
                                               @JsonProperty("adresselinjer") List<String> adresselinjer,
                                               @JsonProperty("postnummer") String postnummer,
                                               @JsonProperty("telefon") String telefon,
                                               @JsonProperty("orgnr") String orgnr,
                                               @JsonProperty("endretAv") String endretAv,
                                               @JsonProperty("endretDato") LocalDate endretDato) {
        this.id = id;
        this.navn = navn;
        this.adresselinjer = adresselinjer;
        this.postnummer = postnummer;
        this.telefon = telefon;
        this.orgnr = orgnr;
        this.endretAv = endretAv;
        this.endretDato = endretDato;
    }

    public String getId() {
        return id;
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
}
