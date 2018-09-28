package no.nav.melosys.domain;

public enum VilkaarType implements Kodeverk {

    INNGANGSVILKÅR("INNGANGSVILKAAR", ""),
    ART12_1("ART12_1", ""),
    FORUTGÅENDE_MEDLEMSKAP("FORUTGAAENDE_MEDLEMSKAP", ""),
    VESENTLIGVIRKSOMHET("VESENTLIGVIRKSOMHET", ""),
    BOSATTINORGE("BOSATTINORGE", ""),
    ART16_1("ART16_1", "");

    private String kode;
    private String beskrivelse;

    VilkaarType(String kode, String beskrivelse) {
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
