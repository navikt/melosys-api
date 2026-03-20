package no.nav.melosys.tjenester.gui.dto.utpeking

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Reproduserer feilen: MismatchedInputException når frontend sender
 * {"utpekingsperioder": {}} i stedet for {"utpekingsperioder": []}.
 *
 * Tester den faktiske ObjectMapper-konfigurasjonen fra WebConfig.
 */
class UtpekingsperioderDtoSerializeTest {

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    @Test
    fun `GET - serialiserer tom liste som array, ikke objekt`() {
        val dto = UtpekingsperioderDto(emptyList())

        val json = objectMapper.writeValueAsString(dto)

        json shouldContain """"utpekingsperioder":[]"""
    }

    @Test
    fun `GET - serialiserer liste med perioder korrekt`() {
        val dto = UtpekingsperioderDto(
            listOf(
                UtpekingsperiodeDto(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    "FO_883_2004_ART13_1A",
                    null,
                    "SE"
                )
            )
        )

        val json = objectMapper.writeValueAsString(dto)

        json shouldContain """"utpekingsperioder":["""
        json shouldContain """"lovvalgsland":"SE""""
    }

    @Test
    fun `POST - deserialiserer gyldig array korrekt`() {
        val json = """{"utpekingsperioder":[]}"""

        val dto = objectMapper.readValue<UtpekingsperioderDto>(json)

        dto.utpekingsperioder() shouldBe emptyList()
    }

    @Test
    fun `POST - reproduserer MismatchedInputException naar frontend sender objekt i stedet for array`() {
        val jsonMedObjektIstedetForArray = """{"utpekingsperioder":{}}"""

        shouldThrow<MismatchedInputException> {
            objectMapper.readValue<UtpekingsperioderDto>(jsonMedObjektIstedetForArray)
        }
    }

    @Test
    fun `round-trip - serialisert GET-respons kan deserialiseres som POST-body`() {
        val original = UtpekingsperioderDto(
            listOf(
                UtpekingsperiodeDto(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    "FO_883_2004_ART13_1A",
                    null,
                    "SE"
                )
            )
        )

        val json = objectMapper.writeValueAsString(original)
        val deserialisert = objectMapper.readValue<UtpekingsperioderDto>(json)

        deserialisert.utpekingsperioder().size shouldBe 1
        deserialisert.utpekingsperioder()[0].lovvalgsland() shouldBe "SE"
    }
}
