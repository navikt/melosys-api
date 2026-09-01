package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto

internal fun UtsendtArbeidstakerSkjemaM2MDto.erOffentligArbeidsgiver(): Boolean? {
    val skjemaer = listOfNotNull(skjema, kobletSkjema)
    val registerverdier = skjemaer
        .mapNotNull { it.metadata.erOffentligArbeidsgiver }
        .distinct()
    check(registerverdier.size <= 1) { "Koblede skjemaer har motstridende registerklassifisering" }
    return registerverdier.singleOrNull()
        ?: skjemaer.firstNotNullOfOrNull { it.manueltSvarOmOffentligArbeidsgiver() }
}

private fun UtsendtArbeidstakerSkjemaDto.manueltSvarOmOffentligArbeidsgiver(): Boolean? =
    when (val skjemaData = data) {
        is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ->
            skjemaData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
        is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
            skjemaData.arbeidsgiversData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
        else -> null
    }
