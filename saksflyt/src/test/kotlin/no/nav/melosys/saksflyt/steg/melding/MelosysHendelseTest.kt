package no.nav.melosys.saksflyt.steg.melding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import org.junit.jupiter.api.Test


class MelosysHendelseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `serialize tom hendelse`() {
        val melosysHendelse = MelosysHendelse(HendelseMelding())


        melosysHendelse.toJsonNode().toPrettyString() shouldEqualJson """
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


        melosysHendelse.toJsonNode().toPrettyString() shouldEqualJson """
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
    fun `deserialize HendelseMelding as part of MelosysHendelse`() {
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
    fun `deserialize VedtakHendelseMelding as part of MelosysHendelse`() {
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


        result.melding.shouldBeInstanceOf<VedtakHendelseMelding>().apply {
            folkeregisterIdent shouldBe "12345"
            sakstype shouldBe Sakstyper.TRYGDEAVTALE
            sakstema shouldBe Sakstemaer.TRYGDEAVGIFT
        }
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

    private fun Any.toJsonNode(): JsonNode = objectMapper.valueToTree(this)
}
