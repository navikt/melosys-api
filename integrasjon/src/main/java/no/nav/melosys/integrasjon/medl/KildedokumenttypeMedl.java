package no.nav.melosys.integrasjon.medl;

public enum KildedokumenttypeMedl {
    HENV_SOKNAD("Henv_Soknad"),
    SED("SED"),
    DOKUMENT("DOKUMENT"),
    PortBlank_A1("PortBlank_A1");

    final String kode;

    KildedokumenttypeMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
