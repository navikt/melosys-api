package no.nav.melosys.domain.brev;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum NorskMyndighet {
    HELFO("Helfo", "986965610"),
    SKATTEETATEN("Skatteetaten", "974761076"),
    SKATTEINNKREVER_UTLAND("Skatteinnkrever utland", "992187298");

    private final String navn;
    private final String orgnr;

    NorskMyndighet(String navn, String orgnr) {
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
