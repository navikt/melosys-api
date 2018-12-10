package no.nav.melosys.integrasjon.medl;

public enum DekningMedl {
    UNNTATT("Unntatt"),
    FULL("Full");

    private String kode;

    DekningMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

}
