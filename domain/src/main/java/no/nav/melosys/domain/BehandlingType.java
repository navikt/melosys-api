package no.nav.melosys.domain;

import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BehandlingType implements Kodeverk<BehandlingType> {

    SØKNAD("SKND", "Søknad"),
    UNNTAK_MEDL("UFM", "Unntak medlemskap"),
    KLAGE("KLG", "Klage"),
    REVURDERING("REV", "Revurdering"),
    MELDING_UTL("ML_U", "Melding fra utenlandsk myndighet"),
    PÅSTAND_UTL("PS_U", "Påstand fra utenlandsk myndighet");

    private String kode;
    private String beskrivelse;

    private BehandlingType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingType> {
        @Override
        protected BehandlingType[] getLovligeVerdier() {
            return BehandlingType.values();
        }
    }

}
