package no.nav.melosys.domain;

public enum Medlemskapstype implements Kodeverk {

    FRIVILLIG("FRIVILLIG", "Frivillig medlemskap"),
    PLIKTIG("PLIKTIG", "Pliktig medlemskap"),
    UNNTATT("UNNTATT", "Unntatt medlemskap");

    private String kode;
    private String beskrivelse;

    Medlemskapstype(String kode, String beskrivelse) {
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
