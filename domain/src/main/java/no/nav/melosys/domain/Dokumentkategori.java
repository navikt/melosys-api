package no.nav.melosys.domain;

public enum Dokumentkategori implements Kodeverk {
    ;

    private String kode;
    private String beskrivelse;

    private Dokumentkategori(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

}
