package no.nav.melosys.domain.brev;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Etat {
    HELFO_ORGNR("HELFO", "986965610"),
    SKATTEETATEN_ORGNR("Skatteetaten", "974761076"),
    SKATTINNKREVER_UTLAND_ORGNR("Skatteinnkrever utland", "992187298");

    private final String navn;
    private final String orgnr;

    Etat(String navn, String orgnr) {
        this.navn = navn;
        this.orgnr = orgnr;
    }

    public String getNavn() {
        return navn;
    }

    public String getOrgnr() {
        return orgnr;
    }
}
