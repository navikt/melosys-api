package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingType implements Kodeverk<BehandlingType> {

    FØRSTEGANGSSØKNAD("NY"),
    ENDRINGSSØKNAD("ENDRING"),
    KLAGE("KLAGE");

    private String kode;

    BehandlingType(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingType> {
        @Override
        protected BehandlingType[] getLovligeVerdier() {
            return BehandlingType.values();
        }
    }

}
