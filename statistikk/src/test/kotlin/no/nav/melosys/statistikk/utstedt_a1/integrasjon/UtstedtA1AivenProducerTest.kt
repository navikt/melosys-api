package no.nav.melosys.statistikk.utstedt_a1.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.json.shouldContainJsonKeyValue
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@SpringBootTest
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class UtstedtA1AivenProducerTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `UtstedtA1Melding serialiserer datoer som ISO-strenger`() {
        val melding = lagMelding()
        val json = objectMapper.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.datoUtstedelse", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.fom", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.tom", "2025-09-01")
    }

    @Test
    fun `UtstedtA1Melding serialiserer artikkel som kode-streng via JsonValue`() {
        val melding = lagMelding()
        val json = objectMapper.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.artikkel", "11.3a")
    }

    @Test
    fun `UtstedtA1Melding serialiserer typeUtstedelse som enum-navn`() {
        val melding = lagMelding()
        val json = objectMapper.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.typeUtstedelse", "FØRSTEGANG")
    }

    private fun lagMelding() = UtstedtA1Melding(
        "MEL-123",
        123L,
        "1234567898765",
        Lovvalgsbestemmelse.ART_11_3_a,
        Periode(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 1)),
        "SE",
        LocalDate.of(2025, 6, 1),
        A1TypeUtstedelse.FØRSTEGANG
    )
}
