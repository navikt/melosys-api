package no.nav.melosys.integrasjon.medl

enum class LovvalgMedl(//"Under avklaring"
    val kode: String
) {
    ENDL("ENDL"),

    //"Endelig"
    FORL("FORL"),

    //"Foreløpig"
    UAVK("UAVK")

}
