package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel12_1 implements Kodeverk {
    IKKE_VESENTLIG_VIRKSOMHET("IKKE_VESENTLIG_VIRKSOMHET", "Foretaket har ikke vesentlig virksomhet"),
    UTSENDELSE_OVER_24_MN("UTSENDELSE_OVER_24_MN", "Utsendelseperioden overskrider 24 måneder"),
    ERSTATTER_ANNEN("ERSTATTER_ANNEN", "Erstatter en annen utsendt person"),
    LOENNET_UTENLANDSK("LOENNET_UTENLANDSK", "Bruker vil bli lønnet av utenlandske arbeidsgiver i utsendelses periode."),
    MEDFØLGENDE_HJEMMEKONTOR("MEDFØLGENDE_HJEMMEKONTOR", "Bruker skal enten være ute under 12 mn eller er medfølgende til en utsendt som er omfattet."),
    IKKE_UTSENDT_PAA_OPPDRAG_FOR_AG("IKKE_UTSENDT_PAA_OPPDRAG_FOR_AG", "Ikke sendt ut for å utføre oppdrag for det norske foretaket"),
    IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER("IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER", "Ikke omfattet av norsk trygd 1 mn før utsendelse"),
    UNDER_2_MN_SIDEN_FORRIGE_UTSENDING_TIL_SAMME_LAND("UNDER_2_MN_SIDEN_FORRIGE_UTSENDING_TIL_SAMME_LAND", "Mindre enn 2 mn siden søker var utsendt for å utføre oppdrag i samme land for samme foretak");

    private String kode;
    private String beskrivelse;

    Artikkel12_1(String kode, String beskrivelse) {
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
