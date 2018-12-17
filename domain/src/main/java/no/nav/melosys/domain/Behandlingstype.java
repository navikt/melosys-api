package no.nav.melosys.domain;

import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Behandlingstype implements InterntKodeverkTabell<Behandlingstype> {

    SØKNAD("SOEKNAD", "Søknad"),
    KLAGE("KLAGE", "Klage"),
    ANKE("ANKE", "Anke"),
    UNNTAK_FRA_MEDLEMSKAP("UNNTAK_FRA_MEDLEMSKAP", "Registrering av unntak"),
    NORGE_UTPEKT("NORGE_UTPEKT", "Behandle at Norge er utpekt"),
    PÅSTAND_UTL("PAASTAND_UTL", "Behandle påstand fra utlandet"),
    NY_VURDERING("NY_VURDERING", "Behandle ny vurdering"),
    ENDRET_PERIODE("ENDRET_PERIODE", "Behandle forkortet periode");

    private String kode;
    private String beskrivelse;

    Behandlingstype(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    @JsonValue
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Converter
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Behandlingstype> {
        @Override
        protected Behandlingstype[] getLovligeVerdier() {
            return Behandlingstype.values();
        }
    }

    /**
     * Henter koder fra felleskodeverk: Behandlingstyper.
     */
    public String hentFellesKode() {
        switch (this) {
            case SØKNAD: return "ae0034";
            case KLAGE: return  "ae0058";
            case NY_VURDERING: return "ae0028";
            default: throw new RuntimeException(this + " er ikke implementert i felleskodeverk.");
        }
    }
}
