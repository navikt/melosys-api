package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto

private enum class OffentligArbeidsgiverKilde {
    BRUKERSVAR,
    ENHETSREGISTERET
}

private data class Versjonsprofil(val offentligArbeidsgiverKilde: OffentligArbeidsgiverKilde)

private val versjonsprofiler = mapOf(
    "1" to Versjonsprofil(OffentligArbeidsgiverKilde.BRUKERSVAR),
    "2" to Versjonsprofil(OffentligArbeidsgiverKilde.ENHETSREGISTERET)
)

private fun versjonsprofil(versjon: String): Versjonsprofil =
    versjonsprofiler[versjon] ?: throw IllegalArgumentException(
        "Ukjent skjemadefinisjonsversjon for utsendt arbeidstaker: $versjon"
    )

private data class OffentligArbeidsgiverVurdering(
    val verdi: Boolean,
    val kilde: OffentligArbeidsgiverKilde
)

internal fun UtsendtArbeidstakerSkjemaM2MDto.erOffentligArbeidsgiver(): Boolean? {
    val vurderinger = listOfNotNull(
        skjema.offentligArbeidsgiverVurdering(),
        kobletSkjema?.offentligArbeidsgiverVurdering()
    )
    val registerverdier = vurderinger
        .filter { it.kilde == OffentligArbeidsgiverKilde.ENHETSREGISTERET }
        .map { it.verdi }
        .distinct()
    check(registerverdier.size <= 1) { "Koblede skjemaer har motstridende registerklassifisering" }
    return registerverdier.singleOrNull()
        ?: vurderinger.firstOrNull { it.kilde == OffentligArbeidsgiverKilde.BRUKERSVAR }?.verdi
}

private fun UtsendtArbeidstakerSkjemaDto.offentligArbeidsgiverVurdering(): OffentligArbeidsgiverVurdering? {
    val kilde = versjonsprofil(skjemaDefinisjonVersjon).offentligArbeidsgiverKilde
    val verdi = when (kilde) {
        OffentligArbeidsgiverKilde.BRUKERSVAR -> when (val skjemaData = data) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ->
                skjemaData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
                skjemaData.arbeidsgiversData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
            else -> null
        }
        OffentligArbeidsgiverKilde.ENHETSREGISTERET -> metadata.erOffentligArbeidsgiver
    }
    return verdi?.let { OffentligArbeidsgiverVurdering(it, kilde) }
}
