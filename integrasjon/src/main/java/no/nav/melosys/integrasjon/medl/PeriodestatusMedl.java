package no.nav.melosys.integrasjon.medl;

public enum PeriodestatusMedl {
    AVST("AVST"),//"Avvist"
    GYLD("GYLD"),//"Gyldig"
    UAVK("UAVK");//"Uavklart"

    private String kode;

    PeriodestatusMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

}
