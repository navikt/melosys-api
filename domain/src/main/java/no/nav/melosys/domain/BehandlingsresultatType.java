package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingsresultatType implements InterntKodeverkTabell<BehandlingsresultatType> {

    ANMODNING_OM_UNNTAK("ANMODNING_OM_UNNTAK", "Anmodning om Unntak"),
    FASTSATT_LOVVALGSLAND("FASTSATT_LOVVALGSLAND", "Lovvalgsland er fastsatt"),
    FORELØPIG_FASTSATT_LOVVALGSLAND("FORELOEPIG_FASTSATT_LOVVALGSLAND", "Lovvalgsland er foreløpig fastsatt"),
    HENLEGGELSE("HENLEGGELSE", "Henleggelse"),
    IKKE_FASTSATT("ANMODNING_OM_UNNTAK", "Anmodning om unntak");

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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Behandlingsstatus> {
        @Override
        protected Behandlingsstatus[] getLovligeVerdier() {
            return Behandlingsstatus.values();
        }
    }
}
