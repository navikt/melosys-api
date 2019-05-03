package no.nav.melosys.integrasjon.medl;

public enum StatusaarsakMedl {
    AVVIST("Avvist"),
    FEILREGISTRERT("Feilregistrert"),
    OPPHORT("Opphort");

    private String kode;

    StatusaarsakMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

}
