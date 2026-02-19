package no.nav.melosys.domain.mottatteopplysninger.utsendtarbeidstaker

data class ArbeidssituasjonOgOevrigUtsendtArbeidstaker(
    val harLoennetArbeidMinstEnMndFoerUtsending: Boolean? = null,
    val beskrivelseArbeidSisteMnd: String? = null,
    val harAndreArbeidsgivereIUtsendingsperioden: Boolean? = null,
    val beskrivelseAnnetArbeid: String? = null,
    val erSkattepliktig: Boolean? = null,
    val mottarYtelserNorge: Boolean? = null,
    val mottarYtelserUtlandet: Boolean? = null
)
