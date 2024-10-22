package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MelosysEessiMeldingTest {

    @Test
    fun `skal takle arbeidssted med null verdi`() {
        val objectMapper = ObjectMapper()

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

        val melding = objectMapper.readValue(json, MelosysEessiMelding::class.java)
        melding.arbeidsland.shouldHaveSize(1)
            .single()
            .arbeidssted shouldBe null
    }
}
