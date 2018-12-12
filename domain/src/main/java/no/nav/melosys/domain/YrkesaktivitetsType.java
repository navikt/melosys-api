package no.nav.melosys.domain;

public enum YrkesaktivitetsType implements Kodeverk {

    LOENNET_ARBEID("LOENNET_ARBEID", "Er i lønnet arbeid"),
    FRILANSER("FRILANSER", "Arbeider som frilanser hos en norsk arbeidsgiver"),
    SELVSTENDIG("SELVSTENDIG", "Arbeider som selvstendig næringsdrivende");

    private String kode;
    private String beskrivelse;

    YrkesaktivitetsType(String kode, String beskrivelse) {
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
