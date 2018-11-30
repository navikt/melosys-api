package no.nav.melosys.integrasjon.medl;

public enum LovvalgMedl {
    AVST("Avvist"),
    GYLD("Gyldig"),
    UAVK("Uavklart");


    private String kode;

    LovvalgMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }
}
