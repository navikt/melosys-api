package no.nav.melosys.integrasjon.medl;

public enum KildedokumenttypeMedl {
    HENV_SOKNAD("Henv_Soknad"),
    SED("SED");

    String kode;

    KildedokumenttypeMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
