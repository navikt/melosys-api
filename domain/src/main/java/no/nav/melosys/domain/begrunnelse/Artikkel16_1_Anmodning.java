package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel16_1_Anmodning implements Kodeverk {
    UTSENDELSE_MELLOM_24_MN_OG_5_AAR("UTSENDELSE_MELLOM_24_MN_OG_5_AAR", "Utsendelseperioden er mer enn 24 mn og mindre enn 5 år."),
    LOENNET_UTL_ENHET_I_KONSERN("UTSENDELSE_FOR_LANG", "Varighet utsendelse overskrider fastsatt lengden i forordningen."),
    ERSTATTER_ANNEN("ERSTATTER_ANNEN", "Arbeidstaker erstatter en annen utsendt arbeidstaker."),
    LOENNET_UTENLANDSK("LOENNET_UTENLANDSK", "Bruker vil bli lønnet av utenlandske arbeidsgiver i utsendelses periode."),
    MEDFØLGENDE_HJEMMEKONTOR("MEDFØLGENDE_HJEMMEKONTOR", "Bruker skal enten være ute under 12 mn eller er medfølgende til en utsendt som er omfattet.");

    private String kode;
    private String beskrivelse;

    Artikkel16_1_Anmodning(String kode, String beskrivelse) {
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
