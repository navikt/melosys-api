package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.sak.FullmektigDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata

/**
 * Aktørene som skal ligge på fagsaken etter at denne innsendingen er behandlet.
 * BRUKER (arbeidstaker) håndteres separat via OpprettSakRequest.medAktørID,
 * så denne strukturen dekker kun ARBEIDSGIVER- og FULLMEKTIG-aktører.
 *
 * Se arkitektur-aktorer-MELOSYS-8029.md for mapping-tabellen.
 */
data class AktørerFraSøknad(
    val arbeidsgiverOrgnumre: List<String>,
    val fullmektige: List<FullmektigDto>
)

object DigitalSøknadAktørerMapper {

    fun utled(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): AktørerFraSøknad {
        return AktørerFraSøknad(
            arbeidsgiverOrgnumre = utledArbeidsgivere(søknadsdata),
            fullmektige = utledFullmektige(søknadsdata)
        )
    }

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

    private fun utledFullmektige(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): List<FullmektigDto> {
        val metadata = søknadsdata.skjema.metadata
        val arbeidsgiverOrgnr = søknadsdata.skjema.orgnr
        return when (metadata) {
            is DegSelvMetadata,
            is ArbeidsgiverMetadata,
            is RadgiverMetadata -> emptyList()

            is AnnenPersonMetadata -> listOf(
                personFullmektig(metadata.fullmektigFnr, Fullmaktstype.FULLMEKTIG_SØKNAD)
            )

            is ArbeidsgiverMedFullmaktMetadata -> listOf(
                personFullmektig(metadata.fullmektigFnr, Fullmaktstype.FULLMEKTIG_SØKNAD),
                organisasjonFullmektig(arbeidsgiverOrgnr, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )

            is RadgiverMedFullmaktMetadata -> listOf(
                personFullmektig(metadata.fullmektigFnr, Fullmaktstype.FULLMEKTIG_SØKNAD),
                organisasjonFullmektig(metadata.radgiverfirma.orgnr, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )
        }
    }

    private fun personFullmektig(fnr: String, fullmaktstype: Fullmaktstype): FullmektigDto =
        FullmektigDto(orgnr = null, personident = fnr, fullmakter = listOf(fullmaktstype))

    private fun organisasjonFullmektig(orgnr: String, fullmaktstype: Fullmaktstype): FullmektigDto =
        FullmektigDto(orgnr = orgnr, personident = null, fullmakter = listOf(fullmaktstype))
}

internal fun FullmektigDto.tilAktoerDto(): AktoerDto = AktoerDto().apply {
    rolleKode = Aktoersroller.FULLMEKTIG.name
    orgnr = this@tilAktoerDto.orgnr
    personIdent = personident
    fullmakter = this@tilAktoerDto.fullmakter.toSet()
}
