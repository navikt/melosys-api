package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel12_2 implements Kodeverk {
    DRIVER_IKKE_VIKRSOMHET_NORGE("DRIVER_IKKE_VIKRSOMHET_NORGE", "Bruker driver vanligvis ikke selvstendig virksomhet i Norge."),
    UTSENDELSE_FOR_LANG("UTSENDELSE_FOR_LANG", "Varighet utsendelse overskrider fastsatt lengden i forordningen.");

    private String kode;
    private String beskrivelse;

    Artikkel12_2(String kode, String beskrivelse) {
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