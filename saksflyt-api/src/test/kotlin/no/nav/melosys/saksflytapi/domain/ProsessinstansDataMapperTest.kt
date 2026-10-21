package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID

/**
 * Serialiseringskontrakten for skjema-api-typene testes i melosys-skjema-api-types. Her testes kun
 * at [Prosessinstans.dataMapper] sin konfigurasjon håndterer typene, og at søknadsdata lagret i
 * PROSESSINSTANS.DATA av en eldre versjon kan leses etter oppgradering.
 */
class ProsessinstansDataMapperTest {

    private val mapper = Prosessinstans.dataMapper

    private val utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
        utsendelseLand = LandKode.SE,
        utsendelsePeriode = PeriodeDto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
    )

    @Test
    fun `søknadsdata overlever round-trip via Prosessinstans`() {
        val prosessinstans = Prosessinstans.forTest()
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = RadgiverMedFullmaktMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Arbeidsgiver AS",
                juridiskEnhetOrgnr = "987654321",
                fullmektigFnr = "11223344556",
                arbeidstakerNavn = "Test Arbeidstaker",
                erOffentligArbeidsgiver = true,
                antallAnsatte = 7,
                kobletSkjemaId = UUID.randomUUID(),
                radgiverfirma = RadgiverfirmaInfo(orgnr = "999888777", navn = "Rådgiver AS")
            )
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                utsendingsperiodeOgLand = utsendingsperiodeOgLand,
                arbeidssituasjon = ArbeidssituasjonDto(
                    harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                    aktivitetIMaanedenFoerUtsendingen = "Jobbet i Norge",
                    skalJobbeForFlereVirksomheter = false,
                    virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                )
            )
            medKobletArbeidsgiverSkjema {
                data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                    utsendingsperiodeOgLand = utsendingsperiodeOgLand,
                    arbeidstakerensLonn = ArbeidstakerensLonnDto(
                        arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                        virksomheterSomUtbetalerLonnOgNaturalytelser = null
                    )
                )
            }
            medVedlegg(lagVedleggDto())
        }

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNADSDATA, dto)

        prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA) shouldBe dto
    }

    @Test
    fun `eldre lagret skjema uten nye felt bruker bakoverkompatible standardverdier`() {
        val metadata = DegSelvMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42
        )
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto { this.metadata = metadata }
        val json = mapper.valueToTree<JsonNode>(dto)
        fjernFelterRekursivt(json, "skjemaDefinisjonVersjon", "erOffentligArbeidsgiver")

        val hentet = mapper.treeToValue(json, UtsendtArbeidstakerSkjemaM2MDto::class.java)

        hentet shouldBe dto.copy(
            skjema = dto.skjema.copy(
                skjemaDefinisjonVersjon = "1",
                metadata = metadata.copy(erOffentligArbeidsgiver = null)
            )
        )
    }

    private fun fjernFelterRekursivt(node: JsonNode, vararg feltnavn: String) {
        if (node is ObjectNode) {
            feltnavn.forEach { node.remove(it) }
        }
        node.forEach { fjernFelterRekursivt(it, *feltnavn) }
    }
}
