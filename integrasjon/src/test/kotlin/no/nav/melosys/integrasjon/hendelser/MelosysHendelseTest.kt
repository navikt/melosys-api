package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate


class MelosysHendelseTest {
    val objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())

    @Test
    fun `serialize tom hendelse`() {
        val melosysHendelse = MelosysHendelse(HendelseMelding())


        melosysHendelse.toJson() shouldEqualJson """
            {
                "melding": {
                    "type": "HendelseMelding"
                }
            }
            """
    }

    @Test
    fun `serialize VedtakHendelseMelding`() {
        val melosysHendelse = MelosysHendelse(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                medlemskapsperiode = Periode(
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2022, 1, 1),
                )
            )
        )


        melosysHendelse.toJson() shouldEqualJson """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                      "medlemskapsperiode": {
                          "fom": [2021, 1, 1],
                          "tom": [2022, 1, 1]
                    }
                  }
            }"""
    }


    @Test
    fun `deserialize HendelseMelding`() {
        val json = """
            {
                "melding": {
                    "type": "HendelseMelding"
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)


        result.melding.shouldBeInstanceOf<HendelseMelding>()
    }

    @Test
    fun `deserialize VedtakHendelseMelding`() {
        val json = """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                    "medlemskapsperiode": null
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)

        result.melding.shouldBe(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                medlemskapsperiode = null
            )
        )    }

    @Test
    fun `deserialize og ignorer ekstra felter i VedtakHendelseMelding`() {
        val json = """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                    "medlemskapsperiode": null,
                    "ekstarfelt": "DUMMY"
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)


        result.melding.shouldBe(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                medlemskapsperiode = null
            )
        )
    }

    data class DummyMelding(
        val folkeregisterIdent: String,
        val sakstype: Sakstyper,
        val nyFelt: String = "default"
    ) : HendelseMelding()

    @Test
    fun `deserialize og legg til default ved manglende`() {
        objectMapper.registerSubtypes(DummyMelding::class.java)

        val json = """
            {
                "melding": {
                    "type": "${"MelosysHendelseTest\$DummyMelding"}",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE"
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)


        result.melding.shouldBe(
            DummyMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                nyFelt = "default"
            )
        )
    }


    // Eksempel på hvordan man kan håndtere ukjente meldinger når man leser køen
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = UkjentMelding::class)
    @JsonSubTypes(
        JsonSubTypes.Type(value = HendelseMeldingForTest::class, name = "HendelseMeldingForTest"),
    )
    open class HendelseMeldingForTest

    data class UkjentMelding(
        val properties: MutableMap<String, Any> = mutableMapOf()
    ) : HendelseMeldingForTest() {

        @JsonAnySetter
        fun setAdditionalProperty(name: String, value: Any) {
            properties[name] = value
        }
    }

    @Test
    fun `retuner UkjentMelding når vi ikke har type`() {

        val json = """{
                    "type": "VedtakHendelseMeldingV2",
                     "pnr": "12345"
                }"""

        val result = objectMapper.readValue<HendelseMeldingForTest>(json)

        result.shouldBeInstanceOf<UkjentMelding>()
            .properties.shouldBe(mapOf("pnr" to "12345"))
    }

    private fun Any.toJson(): String = objectMapper.valueToTree<JsonNode?>(this).toPrettyString()
}
