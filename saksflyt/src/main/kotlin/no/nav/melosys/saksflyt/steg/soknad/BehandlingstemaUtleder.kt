package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto

fun utledBehandlingstema(dto: UtsendtArbeidstakerSkjemaM2MDto): Behandlingstema {
    val erOffentligVirksomhet = when (val data = dto.skjema.data) {
        is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ->
            data.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
        is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
            data.arbeidsgiversData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
        else -> false
    }
    return if (erOffentligVirksomhet == true) Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
    else Behandlingstema.UTSENDT_ARBEIDSTAKER
}

