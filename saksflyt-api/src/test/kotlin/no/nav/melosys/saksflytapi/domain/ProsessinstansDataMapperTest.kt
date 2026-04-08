package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import org.junit.jupiter.api.Test
import java.util.UUID

class ProsessinstansDataMapperTest {

    private val mapper = Prosessinstans.dataMapper

    // --- UtsendtArbeidstakerMetadata polymorfisme ---

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som DegSelvMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "arbeidsgiverNavn": "TestAS",
              "juridiskEnhetOrgnr": "123456789"
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result.shouldBeInstanceOf<DegSelvMetadata>().also {
            it.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
            it.arbeidsgiverNavn shouldBe "TestAS"
            it.juridiskEnhetOrgnr shouldBe "123456789"
        }
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som ArbeidsgiverMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER",
              "skjemadel": "ARBEIDSGIVERS_DEL",
              "arbeidsgiverNavn": "Arbeidsgiver AS",
              "juridiskEnhetOrgnr": "987654321"
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result.shouldBeInstanceOf<ArbeidsgiverMetadata>().also {
            it.skjemadel shouldBe Skjemadel.ARBEIDSGIVERS_DEL
            it.arbeidsgiverNavn shouldBe "Arbeidsgiver AS"
        }
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som AnnenPersonMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "arbeidsgiverNavn": "TestAS",
              "juridiskEnhetOrgnr": "123456789",
              "fullmektigFnr": "12345678901"
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result.shouldBeInstanceOf<AnnenPersonMetadata>().also {
            it.fullmektigFnr shouldBe "12345678901"
        }
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som ArbeidsgiverMedFullmaktMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_MED_FULLMAKT",
              "skjemadel": "ARBEIDSGIVERS_DEL",
              "arbeidsgiverNavn": "Arbeidsgiver AS",
              "juridiskEnhetOrgnr": "123456789",
              "fullmektigFnr": "98765432109"
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result.shouldBeInstanceOf<ArbeidsgiverMedFullmaktMetadata>().also {
            it.fullmektigFnr shouldBe "98765432109"
        }
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerMetadata som RadgiverMedFullmaktMetadata`() {
        val json = """
            {
              "metadatatype": "UTSENDT_ARBEIDSTAKER_RADGIVER_MED_FULLMAKT",
              "skjemadel": "ARBEIDSTAKERS_DEL",
              "arbeidsgiverNavn": "TestAS",
              "juridiskEnhetOrgnr": "123456789",
              "fullmektigFnr": "11223344556",
              "radgiverfirma": {
                "orgnr": "999888777",
                "navn": "Rådgiver AS"
              }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerMetadata::class.java)

        result.shouldBeInstanceOf<RadgiverMedFullmaktMetadata>().also {
            it.fullmektigFnr shouldBe "11223344556"
            it.radgiverfirma.navn shouldBe "Rådgiver AS"
        }
    }

    // --- UtsendtArbeidstakerSkjemaData polymorfisme ---

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaData som arbeidstakers del`() {
        val json = """{"type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"}"""

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaData::class.java)

        result.shouldBeInstanceOf<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>()
    }

    @Test
    fun `deserialiser UtsendtArbeidstakerSkjemaData som arbeidsgivers del`() {
        val json = """{"type": "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL"}"""

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaData::class.java)

        result.shouldBeInstanceOf<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>()
    }

    // --- UtsendtArbeidstakerSkjemaDto round-trip ---

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
              "metadata": {
                "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
                "skjemadel": "ARBEIDSTAKERS_DEL",
                "arbeidsgiverNavn": "NorgeAS",
                "juridiskEnhetOrgnr": "987654321"
              },
              "data": {
                "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"
              }
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaDto::class.java)

        result.id shouldBe skjemaId
        result.status shouldBe SkjemaStatus.SENDT
        result.type shouldBe SkjemaType.UTSENDT_ARBEIDSTAKER
        result.fnr shouldBe "12345678901"
        result.metadata.shouldBeInstanceOf<DegSelvMetadata>()
        result.data.shouldBeInstanceOf<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>()
    }

    // --- SkjemaMottattMelding ---

    @Test
    fun `deserialiser SkjemaMottattMelding`() {
        val skjemaId = UUID.randomUUID()
        val json = """{"skjemaId": "$skjemaId"}"""

        val result = mapper.readValue(json, SkjemaMottattMelding::class.java)

        result.skjemaId shouldBe skjemaId
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
                  "arbeidsgiverNavn": "TestAS",
                  "juridiskEnhetOrgnr": "987654321"
                },
                "data": {
                  "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"
                }
              },
              "referanseId": "ref-123",
              "tidligereInnsendteSkjema": [],
              "innsendtTidspunkt": "2024-01-01T12:00:00",
              "innsenderFnr": "12345678901",
              "vedlegg": []
            }
        """.trimIndent()

        val result = mapper.readValue(json, UtsendtArbeidstakerSkjemaM2MDto::class.java)

        result.skjema?.id shouldBe skjemaId
        result.referanseId shouldBe "ref-123"
        result.skjema?.metadata.shouldBeInstanceOf<DegSelvMetadata>()
    }
}
