package no.nav.melosys.domain.person

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SivilstandstypeTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `deserialiserer fra enum-konstantnavn`() {
        val json = "\"UOPPGITT\""

        val type: Sivilstandstype = objectMapper.readValue(json)

        type shouldBe Sivilstandstype.UOPPGITT
    }

    @Test
    fun `serialiserer til enum-konstantnavn`() {
        val json = objectMapper.writeValueAsString(Sivilstandstype.UOPPGITT)

        json shouldBe "\"UOPPGITT\""
    }

    @Test
    fun `round-trip for alle verdier`() {
        for (type in Sivilstandstype.entries) {
            val json = objectMapper.writeValueAsString(type)
            val deserialisert: Sivilstandstype = objectMapper.readValue(json)

            deserialisert shouldBe type
        }
    }

    @Test
    fun `toString returnerer lesbart navn`() {
        Sivilstandstype.UOPPGITT.toString() shouldBe "Uoppgitt"
        Sivilstandstype.ENKE_ELLER_ENKEMANN.toString() shouldBe "Enke eller enkemann"
        Sivilstandstype.GJENLEVENDE_PARTNER.toString() shouldBe "Gjenlevende partner"
    }
}
