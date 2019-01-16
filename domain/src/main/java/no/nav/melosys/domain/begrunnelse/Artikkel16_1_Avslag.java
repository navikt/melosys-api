package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel16_1_Avslag implements Kodeverk {
    SOEKT_FOR_SENT("SOEKT_FOR_SENT", "Søkt mer enn ett år etter perioden startet"),
    ERSTATTER_EN_ANNEN_SAMLET_OVER_5_AAR("ERSTATTER_EN _ANNEN_SAMLET_OVER_5_AAR", "Erstatter en annen utsendt person, samlet periode mer enn 5 år"),
    FORLENGELSE_SAMLET_OVER_5_AAR("FORLENGELSE_SAMLET_OVER_5_AAR", "Søkt om forlengelse, samlet periode mer enn fem år."),
    INGEN_SPESIELLE_FORHOLD("INGEN_SPESIELLE_FORHOLD", "Ingen spesielle forhold"),
    SAERLIG_AVSLAGSGRUNN("SAERLIG_AVSLAGSGRUNN", "Særlig avslagsgrunn");

    private String kode;
    private String beskrivelse;

    Artikkel16_1_Avslag(String kode, String beskrivelse) {
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
}
