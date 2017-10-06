package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingType implements Kodeverk<BehandlingType> {

    SØKNAD("SØKNAD", "Behandling av søknad"),
    KLAGE("KLAGE", "Behandling av klage"),
    MELDING_UTL("MELDING_UTL", "Behandling av meldinger fra utenlandske myndigheter"),
    PÅSTAND_UTL("PÅSTAND_UTL", "Behandling av påstander fra utenlandske myndigheter");

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
