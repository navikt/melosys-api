package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsinntektKilde
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.InntektType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ProsessinstansDataMapperTest {

    private val mapper = Prosessinstans.dataMapper

    private val kobletSkjemaId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val erstatterSkjemaId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private val fellesMetadataFelter = """
        "arbeidsgiverNavn": "Arbeidsgiver AS",
        "juridiskEnhetOrgnr": "987654321",
        "arbeidstakerNavn": "Test Arbeidstaker",
        "erOffentligArbeidsgiver": true,
        "antallAnsatte": 42,
        "erOffentligTjenesteperson": false,
        "kobletSkjemaId": "$kobletSkjemaId",
        "erstatterSkjemaId": "$erstatterSkjemaId"
    """.trimIndent()

    private val utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
        utsendelseLand = LandKode.SE,
        utsendelsePeriode = PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
    )

    private val utsendingsperiodeOgLandJson = """
        "utsendingsperiodeOgLand": {
          "utsendelseLand": "SE",
          "utsendelsePeriode": { "fraDato": "2024-01-01", "tilDato": "2024-12-31" }
        }
    """.trimIndent()

    // --- UtsendtArbeidstakerMetadata polymorfisme ---

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som DegSelvMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe DegSelvMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som ArbeidsgiverMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER",
              "skjemadel": "ARBEIDSGIVERS_DEL",
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe ArbeidsgiverMetadata(
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som AnnenPersonMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "fullmektigFnr": "12345678901",
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe AnnenPersonMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            fullmektigFnr = "12345678901",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som ArbeidsgiverMedFullmaktMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_MED_FULLMAKT",
              "skjemadel": "ARBEIDSGIVERS_DEL",
              "fullmektigFnr": "98765432109",
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe ArbeidsgiverMedFullmaktMetadata(
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            fullmektigFnr = "98765432109",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som RadgiverMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_RADGIVER",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "radgiverfirma": { "orgnr": "999888777", "navn": "Rådgiver AS" },
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe RadgiverMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId,
            radgiverfirma = RadgiverfirmaInfo(orgnr = "999888777", navn = "Rådgiver AS")
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som RadgiverMedFullmaktMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_RADGIVER_MED_FULLMAKT",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "fullmektigFnr": "11223344556",
              "radgiverfirma": { "orgnr": "999888777", "navn": "Rådgiver AS" },
              $fellesMetadataFelter
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result shouldBe RadgiverMedFullmaktMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Arbeidsgiver AS",
            juridiskEnhetOrgnr = "987654321",
            fullmektigFnr = "11223344556",
            arbeidstakerNavn = "Test Arbeidstaker",
            erOffentligArbeidsgiver = true,
            antallAnsatte = 42,
            erOffentligTjenesteperson = false,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId,
            radgiverfirma = RadgiverfirmaInfo(orgnr = "999888777", navn = "Rådgiver AS")
        )
        rundtur(result) shouldBe result
    }

    // --- UtsendtArbeidstakerSkjemaData polymorfisme ---

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaData som arbeidstakers del`() {
        val json = """
            {
              "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL",
              $utsendingsperiodeOgLandJson,
              "arbeidssituasjon": {
                "harVaertEllerSkalVaereILonnetArbeidFoerUtsending": true,
                "aktivitetIMaanedenFoerUtsendingen": "Jobbet i Norge",
                "skalJobbeForFlereVirksomheter": false
              }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaData::class.java)

        result shouldBe UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
            utsendingsperiodeOgLand = utsendingsperiodeOgLand,
            arbeidssituasjon = ArbeidssituasjonDto(
                harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                aktivitetIMaanedenFoerUtsendingen = "Jobbet i Norge",
                skalJobbeForFlereVirksomheter = false,
                virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
            )
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaData som arbeidsgivers del`() {
        val json = """
            {
              "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL",
              $utsendingsperiodeOgLandJson,
              "arbeidstakerensLonn": { "arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden": true }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaData::class.java)

        result shouldBe UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
            utsendingsperiodeOgLand = utsendingsperiodeOgLand,
            arbeidstakerensLonn = ArbeidstakerensLonnDto(
                arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                virksomheterSomUtbetalerLonnOgNaturalytelser = null
            )
        )
        rundtur(result) shouldBe result
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaData som arbeidsgiver og arbeidstakers del`() {
        val json = """
            {
              "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL",
              $utsendingsperiodeOgLandJson,
              "arbeidsgiversData": {
                "arbeidstakerensLonn": { "arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden": false }
              },
              "arbeidstakersData": {
                "arbeidssituasjon": {
                  "harVaertEllerSkalVaereILonnetArbeidFoerUtsending": false,
                  "skalJobbeForFlereVirksomheter": true
                }
              }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaData::class.java)

        result shouldBe UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
            utsendingsperiodeOgLand = utsendingsperiodeOgLand,
            arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                arbeidstakerensLonn = ArbeidstakerensLonnDto(
                    arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
                    virksomheterSomUtbetalerLonnOgNaturalytelser = null
                )
            ),
            arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(
                arbeidssituasjon = ArbeidssituasjonDto(
                    harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
                    aktivitetIMaanedenFoerUtsendingen = null,
                    skalJobbeForFlereVirksomheter = true,
                    virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                )
            )
        )
        rundtur(result) shouldBe result
    }

    // --- UtsendtArbeidstakerSkjemaDto ---

    @Test
    fun `round-trip serialisering av UtsendtArbeidstakerSkjemaDto med metadata`() {
        val skjemaId = UUID.randomUUID()
        val json = """
            {
              "id": "$skjemaId",
              "status": "SENDT",
              "type": "UTSENDT_ARBEIDSTAKER",
              "fnr": "12345678901",
              "orgnr": "123456789",
              "opprettetDato": "2024-01-01T12:00:00",
              "endretDato": "2024-01-02T08:30:00",
              "skjemaDefinisjonVersjon": "2",
              "metadata": {
                "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
                "skjemadel": "ARBEIDSTAKERS_DEL",
                $fellesMetadataFelter
              },
              "data": {
                "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL",
                "skatteforholdOgInntekt": {
                  "erSkattepliktigTilNorgeIHeleutsendingsperioden": true,
                  "mottarPengestotteFraAnnetEosLandEllerSveits": false,
                  "inntektFraNorskEllerUtenlandskVirksomhet": ["NORSK_VIRKSOMHET", "UTENLANDSK_VIRKSOMHET"],
                  "hvilkeTyperInntektHarDu": ["LOENN", "INNTEKT_FRA_EGEN_VIRKSOMHET"],
                  "inntekt": "65000",
                  "inntektFraEgenVirksomhet": "12000"
                }
              }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaDto::class.java)

        result shouldBe UtsendtArbeidstakerSkjemaDto(
            id = skjemaId,
            status = SkjemaStatus.SENDT,
            type = SkjemaType.UTSENDT_ARBEIDSTAKER,
            fnr = "12345678901",
            orgnr = "123456789",
            opprettetDato = LocalDateTime.of(2024, 1, 1, 12, 0),
            endretDato = LocalDateTime.of(2024, 1, 2, 8, 30),
            skjemaDefinisjonVersjon = "2",
            metadata = DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Arbeidsgiver AS",
                juridiskEnhetOrgnr = "987654321",
                arbeidstakerNavn = "Test Arbeidstaker",
                erOffentligArbeidsgiver = true,
                antallAnsatte = 42,
                erOffentligTjenesteperson = false,
                kobletSkjemaId = kobletSkjemaId,
                erstatterSkjemaId = erstatterSkjemaId
            ),
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                skatteforholdOgInntekt = SkatteforholdOgInntektDto(
                    erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
                    mottarPengestotteFraAnnetEosLandEllerSveits = false,
                    landSomUtbetalerPengestotte = null,
                    pengestotteSomMottasFraAndreLandBelop = null,
                    pengestotteSomMottasFraAndreLandBeskrivelse = null,
                    inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
                    hvilkeTyperInntektHarDu = setOf(InntektType.LOENN, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
                    inntekt = "65000",
                    inntektFraEgenVirksomhet = "12000"
                )
            )
        )
        rundtur(result) shouldBe result
    }

    // --- SkjemaMottattMelding ---

    @Test
    fun `deserialiser SkjemaMottattMelding`() {
        val skjemaId = UUID.randomUUID()
        val relatertSkjemaId = UUID.randomUUID()
        val json = """{"skjemaId": "$skjemaId", "relaterteSkjemaIder": ["$relatertSkjemaId"]}"""

        val result = mapper.readValue(json, SkjemaMottattMelding::class.java)

        result shouldBe SkjemaMottattMelding(skjemaId = skjemaId, relaterteSkjemaIder = listOf(relatertSkjemaId))
    }

    // --- UtsendtArbeidstakerSkjemaM2MDto ---

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaM2MDto med skjema`() {
        val skjemaId = UUID.randomUUID()
        val json = """
            {
              "skjema": {
                "id": "$skjemaId",
                "status": "UTKAST",
                "type": "UTSENDT_ARBEIDSTAKER",
                "fnr": "12345678901",
                "orgnr": "123456789",
                "opprettetDato": "2024-01-01T12:00:00",
                "endretDato": "2024-01-02T08:30:00",
                "metadata": {
                  "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
                  "skjemadel": "ARBEIDSTAKERS_DEL",
                  $fellesMetadataFelter
                },
                "data": {
                  "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"
                }
              },
              "referanseId": "ref-123",
              "tidligereInnsendteSkjema": [],
              "innsendtTidspunkt": "2024-01-01T12:00:00",
              "innsenderFnr": "12345678901",
              "dokumentTittel": "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits",
              "vedlegg": []
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaM2MDto::class.java)

        result shouldBe UtsendtArbeidstakerSkjemaM2MDto(
            skjema = UtsendtArbeidstakerSkjemaDto(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                type = SkjemaType.UTSENDT_ARBEIDSTAKER,
                fnr = "12345678901",
                orgnr = "123456789",
                opprettetDato = LocalDateTime.of(2024, 1, 1, 12, 0),
                endretDato = LocalDateTime.of(2024, 1, 2, 8, 30),
                metadata = DegSelvMetadata(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    arbeidsgiverNavn = "Arbeidsgiver AS",
                    juridiskEnhetOrgnr = "987654321",
                    arbeidstakerNavn = "Test Arbeidstaker",
                    erOffentligArbeidsgiver = true,
                    antallAnsatte = 42,
                    erOffentligTjenesteperson = false,
                    kobletSkjemaId = kobletSkjemaId,
                    erstatterSkjemaId = erstatterSkjemaId
                ),
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            ),
            kobletSkjema = null,
            tidligereInnsendteSkjema = emptyList(),
            referanseId = "ref-123",
            innsendtTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
            innsenderFnr = "12345678901",
            dokumentTittel = "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits",
            vedlegg = emptyList()
        )
        rundtur(result) shouldBe result
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

    @Test
    fun `antallAnsatte fra EREG overlever round-trip av søknadsdata via Prosessinstans`() {
        val prosessinstans = Prosessinstans.forTest()
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto { antallAnsatte = 7 }

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNADSDATA, dto)
        val hentet = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)

        hentet shouldBe dto
        hentet.skjema.metadata.antallAnsatte shouldBe 7
    }

    private inline fun <reified T : Any> rundtur(verdi: T): T =
        mapper.readValue(mapper.writeValueAsString(verdi), T::class.java)

    private fun fjernFelterRekursivt(node: JsonNode, vararg feltnavn: String) {
        if (node is ObjectNode) {
            feltnavn.forEach { node.remove(it) }
        }
        node.forEach { fjernFelterRekursivt(it, *feltnavn) }
    }
}
