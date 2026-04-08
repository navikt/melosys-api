package no.nav.melosys.saksflytapi.skjema

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import java.time.LocalDateTime
import java.util.UUID

fun lagUtsendtArbeidstakerSkjemaM2MDto(
    init: UtsendtArbeidstakerSkjemaM2MDtoTestFactory.Builder.() -> Unit = {}
): UtsendtArbeidstakerSkjemaM2MDto =
    UtsendtArbeidstakerSkjemaM2MDtoTestFactory.Builder().apply(init).build()

object UtsendtArbeidstakerSkjemaM2MDtoTestFactory {

    @MelosysTestDsl
    class Builder {
        var fnr: String = "12345678901"
        var orgnr: String = "123456789"
        var juridiskEnhetOrgnr: String = "987654321"
        var arbeidsgiverNavn: String = "Test AS"
        var skjemadel: Skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
        var data: no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData =
            UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
        var referanseId: String = "MEL-${UUID.randomUUID()}"
        var innsenderFnr: String? = null
        var innsendtTidspunkt: LocalDateTime = LocalDateTime.now()

        private var kobletSkjemaBuilder: ArbeidsgiverSkjemaBuilder? = null

        fun medKobletArbeidsgiverSkjema(init: ArbeidsgiverSkjemaBuilder.() -> Unit = {}) {
            kobletSkjemaBuilder = ArbeidsgiverSkjemaBuilder(
                fnr = fnr,
                orgnr = orgnr,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidsgiverNavn = arbeidsgiverNavn
            ).apply(init)
        }

        fun build(): UtsendtArbeidstakerSkjemaM2MDto {
            val skjema = lagSkjemaDto(skjemadel, data)
            val kobletSkjema = kobletSkjemaBuilder?.let {
                lagSkjemaDto(Skjemadel.ARBEIDSGIVERS_DEL, it.data, it.fnr, it.orgnr, it.juridiskEnhetOrgnr, it.arbeidsgiverNavn)
            }

            return UtsendtArbeidstakerSkjemaM2MDto(
                skjema = skjema,
                kobletSkjema = kobletSkjema,
                tidligereInnsendteSkjema = emptyList(),
                referanseId = referanseId,
                innsendtTidspunkt = innsendtTidspunkt,
                innsenderFnr = innsenderFnr ?: fnr
            )
        }

        private fun lagSkjemaDto(
            skjemadel: Skjemadel,
            data: no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData,
            fnr: String = this.fnr,
            orgnr: String = this.orgnr,
            juridiskEnhetOrgnr: String = this.juridiskEnhetOrgnr,
            arbeidsgiverNavn: String = this.arbeidsgiverNavn
        ) = UtsendtArbeidstakerSkjemaDto(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            fnr = fnr,
            orgnr = orgnr,
            opprettetDato = LocalDateTime.now(),
            endretDato = LocalDateTime.now(),
            metadata = when (skjemadel) {
                Skjemadel.ARBEIDSTAKERS_DEL -> DegSelvMetadata(
                    skjemadel = skjemadel,
                    arbeidsgiverNavn = arbeidsgiverNavn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr
                )
                Skjemadel.ARBEIDSGIVERS_DEL -> ArbeidsgiverMetadata(
                    skjemadel = skjemadel,
                    arbeidsgiverNavn = arbeidsgiverNavn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr
                )
                Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> DegSelvMetadata(
                    skjemadel = skjemadel,
                    arbeidsgiverNavn = arbeidsgiverNavn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr
                )
            },
            data = data
        )
    }

    @MelosysTestDsl
    class ArbeidsgiverSkjemaBuilder(
        var fnr: String = "12345678901",
        var orgnr: String = "123456789",
        var juridiskEnhetOrgnr: String = "987654321",
        var arbeidsgiverNavn: String = "Test AS",
        var data: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
    )
}
