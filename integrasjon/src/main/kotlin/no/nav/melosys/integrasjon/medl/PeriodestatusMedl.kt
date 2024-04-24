package no.nav.melosys.integrasjon.medl

enum class PeriodestatusMedl(//"Uavklart"
    @JvmField val kode: String
) {
    AVST("AVST"),

    //"Avvist"
    GYLD("GYLD"),

    //"Gyldig"
    UAVK("UAVK")

}
