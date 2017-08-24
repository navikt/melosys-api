package no.nav.melosys.domain;

import javax.persistence.Converter;

// FIXME (farjam): Ikke revidert for v0

public enum BehandlingsMaate implements Kodeverk<BehandlingsMaate> {

    AUTOMATISERT("AUTO"), 
    DELVIS_AUTO("DELVIS_AUTO"), 
    MANUELT("MANUELT");

    private String kode;

    private BehandlingsMaate(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingsMaate> {
        @Override
        protected BehandlingsMaate[] getLovligeVerdier() {
            return BehandlingsMaate.values();
        }
    }

}
