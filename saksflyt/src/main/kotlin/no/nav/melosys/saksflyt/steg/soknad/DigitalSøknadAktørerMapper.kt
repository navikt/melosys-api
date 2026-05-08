package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel

/**
 * Aktørene som en innsending skal sette opp på saken (BRUKER håndteres separat).
 */
data class AktørerFraSøknad(
    val arbeidsgiverOrgnumre: List<String>,
    val fullmektige: List<FullmektigSpec>,
    val skjemadel: Skjemadel
)

/**
 * En FULLMEKTIG-aktør som skal lagres. orgnr og/eller personIdent er satt.
 * Når orgnr finnes og innsender er identifisert, får vi også [kontaktpersonFnr]
 * som brukes til å slå opp navn til KONTAKTOPPLYSNING via PDL.
 */
data class FullmektigSpec(
    val orgnr: String?,
    val personIdent: String?,
    val kontaktpersonFnr: String?,
    val fullmakter: Set<Fullmaktstype>
) {
    init {
        require(orgnr != null || personIdent != null) {
            "Fullmektig må ha minst orgnr eller personIdent"
        }
        require(orgnr != null || kontaktpersonFnr == null) {
            "Kontaktperson-info krever at fullmektig har orgnr (lagres på (sak, orgnr))"
        }
        require(fullmakter.isNotEmpty()) { "Fullmektig må ha minst én fullmaktstype" }
    }
}

object DigitalSøknadAktørerMapper {

    fun utled(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): AktørerFraSøknad =
        AktørerFraSøknad(
            arbeidsgiverOrgnumre = utledArbeidsgivere(søknadsdata),
            fullmektige = utledFullmektige(søknadsdata),
            skjemadel = søknadsdata.skjema.metadata.skjemadel
        )

    private fun utledArbeidsgivere(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): List<String> {
        // Tar orgnr fra både primært og koblet skjema. AT-del og AG-del kan ha ulike orgnr
        // selv om det er samme juridiske enhet, og da skal begge stå som ARBEIDSGIVER på saken.
        val primær = søknadsdata.skjema.orgnr
        val koblet = søknadsdata.kobletSkjema?.orgnr
        return if (koblet != null && koblet != primær) {
            listOf(primær, koblet)
        } else {
            listOf(primær)
        }
    }

    private fun utledFullmektige(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): List<FullmektigSpec> {
        val metadata = søknadsdata.skjema.metadata
        val innsenderFnr = søknadsdata.innsenderFnr
        val arbeidsgiverOrgnr = søknadsdata.skjema.orgnr

        return when (metadata) {
            is DegSelvMetadata,
            is ArbeidsgiverMetadata -> emptyList()

            is AnnenPersonMetadata -> listOf(
                FullmektigSpec(
                    orgnr = null,
                    personIdent = metadata.fullmektigFnr,
                    kontaktpersonFnr = null,
                    fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
                )
            )

            is ArbeidsgiverMedFullmaktMetadata -> listOf(
                FullmektigSpec(
                    orgnr = arbeidsgiverOrgnr,
                    personIdent = metadata.fullmektigFnr,
                    kontaktpersonFnr = metadata.fullmektigFnr,
                    fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
                )
            )

            is RadgiverMetadata -> listOf(
                FullmektigSpec(
                    orgnr = metadata.radgiverfirma.orgnr,
                    personIdent = null,
                    kontaktpersonFnr = innsenderFnr,
                    fullmakter = setOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
                )
            )

            is RadgiverMedFullmaktMetadata -> listOf(
                FullmektigSpec(
                    orgnr = metadata.radgiverfirma.orgnr,
                    personIdent = metadata.fullmektigFnr,
                    kontaktpersonFnr = metadata.fullmektigFnr,
                    fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
                )
            )
        }
    }
}
