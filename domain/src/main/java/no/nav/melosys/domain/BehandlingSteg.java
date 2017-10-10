package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingSteg implements Kodeverk<BehandlingSteg> {

    // FIXME (farjam EESSI2-291): Venter på tilstandsmodellen
    NY("NY", "Ny"), 
    KLARGJORT("KLARGJORT", "Klargjort");

    private String kode;
    private String beskrivelse;

    private BehandlingSteg(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingSteg> {
        @Override
        protected BehandlingSteg[] getLovligeVerdier() {
            return BehandlingSteg.values();
        }
    }

}
