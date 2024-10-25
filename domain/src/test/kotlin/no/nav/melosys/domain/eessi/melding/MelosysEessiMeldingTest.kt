package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MelosysEessiMeldingTest {

    @Test
    fun `skal takle arbeidssted med null verdi`() {
        val objectMapper = jacksonObjectMapper()

        val json = """
            {
              "arbeidsland": [
                {
                  "land": "Norge",
                  "arbeidssted": null
                }
              ]
            }
        """

        val melding = objectMapper.readValue<MelosysEessiMelding>(json)
        melding.arbeidsland.shouldHaveSize(1)
            .single()
            .arbeidssted shouldBe emptyList()
    }
}
