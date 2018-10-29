package no.nav.melosys.domain.dokument.medlemskap;

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

    public void setKode(String kode) {
        this.kode = kode;
    }
}
