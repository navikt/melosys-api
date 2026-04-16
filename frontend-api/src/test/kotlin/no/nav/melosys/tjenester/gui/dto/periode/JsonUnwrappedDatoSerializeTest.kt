package no.nav.melosys.tjenester.gui.dto.periode

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSkrivDto
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Verifiserer at @JsonUnwrapped med suffix fungerer korrekt i Jackson 3.
 *
 * Både AnmodningsperiodeSkrivDto og LovvalgsperiodeDto bruker:
 *   @JsonUnwrapped(suffix = "Dato")
 *   val periode: PeriodeDto
 *
 * Dette betyr at PeriodeDto sine felter (fom/tom) skal serialiseres som
 * fomDato/tomDato i den ytre klassen. Jackson 3 endret noe av oppførselen
 * rundt @JsonUnwrapped, så vi verifiserer at dette fortsatt fungerer.
 */
class JsonUnwrappedDatoSerializeTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `AnmodningsperiodeSkrivDto - periode serialiseres som fomDato og tomDato`() {
        val json = mapOf(
            "id" to "1",
            "fomDato" to "2024-01-01",
            "tomDato" to "2024-12-31",
            "lovvalgsland" to "SE"
        )
        val dto = AnmodningsperiodeSkrivDto(json.mapValues { it.value as Any? }
            .let {
                @Suppress("UNCHECKED_CAST")
                json as Map<String, String>
            })

        val serialized = objectMapper.writeValueAsString(dto)

        serialized shouldContain "fomDato"
        serialized shouldContain "tomDato"
        serialized shouldContain "2024-01-01"
        serialized shouldNotContain """"fom":"""
        serialized shouldNotContain """"tom":"""
        serialized shouldNotContain """"periode":"""
    }

    @Test
    fun `AnmodningsperiodeSkrivDto - round-trip via @JsonCreator map-konstruktør`() {
        val originalJson = """
            {
              "id": "42",
              "fomDato": "2024-01-01",
              "tomDato": "2024-12-31",
              "lovvalgBestemmelse": "FO_883_2004_ART13_1A",
              "lovvalgsland": "SE"
            }
        """.trimIndent()

        val dto = objectMapper.readValue<AnmodningsperiodeSkrivDto>(originalJson)
        val serialized = objectMapper.writeValueAsString(dto)

        serialized shouldContain "fomDato"
        serialized shouldContain "2024-01-01"
        serialized shouldContain "2024-12-31"
    }

    @Test
    fun `LovvalgsperiodeDto - periode serialiseres som fomDato og tomDato`() {
        val dto = LovvalgsperiodeDto(
            "p1",
            PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
            null, null, null,
            no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET,
            null, null, null
        )

        val serialized = objectMapper.writeValueAsString(dto)

        serialized shouldContain "fomDato"
        serialized shouldContain "tomDato"
        serialized shouldContain "2024-01-01"
        serialized shouldNotContain """"fom":"""
        serialized shouldNotContain """"periode":"""
    }

    @Test
    fun `LovvalgsperiodeDto - round-trip via @JsonCreator map-konstruktør`() {
        val json = """
            {
              "fomDato": "2024-01-01",
              "tomDato": "2024-12-31",
              "lovvalgsbestemmelse": "FO_883_2004_ART12_1",
              "tilleggBestemmelse": "FO_883_2004_ART11_2",
              "unntakFraBestemmelse": "FO_883_2004_ART11_1",
              "innvilgelsesResultat": "INNVILGET",
              "lovvalgsland": "NO",
              "unntakFraLovvalgsland": "NO",
              "trygdeDekning": "FULL_DEKNING_EOSFO",
              "medlemskapstype": "PLIKTIG",
              "medlemskapsperiodeID": "20"
            }
        """.trimIndent()

        val dto = objectMapper.readValue<LovvalgsperiodeDto>(json)
        val serialized = objectMapper.writeValueAsString(dto)

        serialized shouldContain "fomDato"
        serialized shouldContain "2024-01-01"
        serialized shouldContain "2024-12-31"
    }
}
