package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingsresultatType implements InterntKodeverkTabell<BehandlingsresultatType> {

    HENLEGGELSE("HENLEGGELSE", "Henleggelsesgrunner"),
    FASTSATT_LOVVALGSLAND("FASTSATT_LOVVALGSLAND", "Fastsatt lovvalgsland"),
    FRIVILLIG_MEDLEMSKAP("FRIVILLIG_MEDLEMSKAP", "Frivillig medlemskap");

    private String kode;
    private String beskrivelse;

    BehandlingsresultatType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<BehandlingStatus> {
        @Override
        protected BehandlingStatus[] getLovligeVerdier() {
            return BehandlingStatus.values();
        }
    }
}
