package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import org.junit.jupiter.api.Test

class MelosysHendelseTest {
    private val objectMapper = jacksonObjectMapper()

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
                sakstema = Sakstemaer.TRYGDEAVGIFT
            )
        )


        melosysHendelse.toJson() shouldEqualJson """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT"
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
                    "sakstema": "TRYGDEAVGIFT"
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)


        result.melding.shouldBe(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT
            )
        )
    }

    @Test
    fun `deserialize og ignorer ekstra felter i VedtakHendelseMelding`() {
        val json = """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                    "ekstarfelt": "DUMMY"
                }
            }"""


        val result = objectMapper.readValue<MelosysHendelse>(json)


        result.melding.shouldBe(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT
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


    @Test
    fun `retuner UkjentMelding når vi ikke har type`() {
        val json = """{
                "melding": {
                    "type": "VedtakHendelseMeldingV2",
                     "pnr": "12345"
                }
            }"""

        val result = objectMapper.readValue<MelosysHendelse>(json)

        result.melding.shouldBeInstanceOf<UkjentMelding>()
            .properties.shouldBe(mapOf("pnr" to "12345"))
    }

    private fun Any.toJson(): String = objectMapper.valueToTree<JsonNode?>(this).toPrettyString()
}
