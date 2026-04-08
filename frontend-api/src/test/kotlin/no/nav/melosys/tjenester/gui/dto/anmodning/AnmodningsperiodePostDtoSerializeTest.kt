package no.nav.melosys.tjenester.gui.dto.anmodning

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/**
 * Tester at Jackson korrekt deserialiserer AnmodningsperiodePostDto ved å bruke
 * no-arg-konstruktøren og ikke single-arg-konstruktøren (Collection<Anmodningsperiode>).
 *
 * I Jackson 3 vil en single-arg-konstruktør med parameternavn (via -parameters) bli
 * behandlet som en "properties creator". Det fører til at Jackson prøver å bygge
 * Anmodningsperiode domain-objekter fra JSON, og kaller getId().toString() på objekter
 * uten database-id (null), noe som gir NullPointerException.
 *
 * Reproduserer e2e-feilen: "Cannot invoke Long.toString() because getId() is null".
 */
class AnmodningsperiodePostDtoSerializeTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `deserialiserer perioder uten id - reproduserer e2e-feilen for nye perioder`() {
        val json = """
            {
              "anmodningsperioder": [
                {
                  "id": null,
                  "fomDato": "2024-01-01",
                  "tomDato": "2024-12-31",
                  "lovvalgBestemmelse": "FO_883_2004_ART13_1A",
                  "lovvalgsland": "SE"
                }
              ]
            }
        """.trimIndent()

        val dto = objectMapper.readValue<AnmodningsperiodePostDto>(json)

        dto.anmodningsperioder shouldHaveSize 1
        dto.anmodningsperioder[0].id shouldBe null
        dto.anmodningsperioder[0].lovvalgsland shouldBe "SE"
    }

    @Test
    fun `deserialiserer tom liste`() {
        val json = """{"anmodningsperioder": []}"""

        val dto = objectMapper.readValue<AnmodningsperiodePostDto>(json)

        dto.anmodningsperioder shouldHaveSize 0
    }

    @Test
    fun `deserialiserer perioder med id`() {
        val json = """
            {
              "anmodningsperioder": [
                {
                  "id": "42",
                  "fomDato": "2024-01-01",
                  "tomDato": null,
                  "lovvalgBestemmelse": "FO_883_2004_ART13_1A",
                  "lovvalgsland": "NO"
                }
              ]
            }
        """.trimIndent()

        val dto = objectMapper.readValue<AnmodningsperiodePostDto>(json)

        dto.anmodningsperioder shouldHaveSize 1
        dto.anmodningsperioder[0].id shouldBe "42"
    }

    @Test
    fun `round-trip - serialisert dto kan deserialiseres`() {
        val original = AnmodningsperiodePostDto()
        original.anmodningsperioder = listOf(
            objectMapper.readValue(
                """{"id":"1","fomDato":"2024-01-01","tomDato":"2024-12-31","lovvalgsland":"SE"}"""
            )
        )

        val json = objectMapper.writeValueAsString(original)
        val deserialisert = objectMapper.readValue<AnmodningsperiodePostDto>(json)

        deserialisert.anmodningsperioder shouldHaveSize 1
        deserialisert.anmodningsperioder[0].id shouldNotBe null
    }
}
