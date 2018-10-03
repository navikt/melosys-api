package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel12_1 implements Kodeverk {
    IKKE_VESENTLIGVIRKSOMHET("IKKE_VESENTLIGVIRKSOMHET", "Arbeidsgiver har ikke vesentlig virksomhet."),
    UTSENDELSE_FOR_LANG("UTSENDELSE_FOR_LANG", "Varighet utsendelse overskrider fastsatt lengden i forordningen."),
    ERSTATTER_ANNEN("ERSTATTER_ANNEN", "Arbeidstaker erstatter en annen utsendt arbeidstaker."),
    LOENNET_UTENLANDSK("LOENNET_UTENLANDSK", "Bruker vil bli lønnet av utenlandske arbeidsgiver i utsendelses periode."),
    MEDFØLGENDE_HJEMMEKONTOR("MEDFØLGENDE_HJEMMEKONTOR", "Bruker skal enten være ute under 12 mn eller er medfølgende til en utsendt som er omfattet.");

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
