package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingType implements Kodeverk<BehandlingType> {

    SØKNAD("ae0034", "Søknad"),
    UNNTAK_MEDL("UFM", "Unntak medlemskap"),
    KLAGE("ae0058", "Klage"),
    REVURDERING("ae0028", "Revurdering"),
    MELDING_UTL("MELDING_UTL", "Melding fra utenlandsk myndighet"),
    PÅSTAND_UTL("PÅSTAND_UTL", "Påstand fra utenlandsk myndighet");

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
