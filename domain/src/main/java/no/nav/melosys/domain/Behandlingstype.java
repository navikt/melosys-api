package no.nav.melosys.domain;

import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Behandlingstype implements InterntKodeverkTabell<Behandlingstype> {

    SØKNAD("SOEKNAD", "Behandling av søknad"),
    KLAGE("KLAGE", "Behandling av klage eller anke"),
    NORGE_UTPEKT("NORGE_UTPEKT", "Behandling av at Norge er utpekt fra utenlandske myndigheter"),
    PÅSTAND_UTL("PAASTAND_UTL", "Behandling av påstand fra utenlandske myndigheter"),
    POSTING_UTL("POSTING_UTL", "Behandling av melding om posting fra utenlandske myndigheter"),
    REVURDERING("REVURDERING", "Behandling av revurdering av et tidligere vedtak");

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
            case REVURDERING: return "ae0028";
            default: throw new RuntimeException(this + " er ikke implementert i felleskodeverk.");
        }
    }
}
