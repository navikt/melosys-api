package no.nav.melosys.statistikk.utstedt_a1.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.shouldNotBe
import jakarta.annotation.Nullable
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import java.time.LocalDate

class UtstedtA1AivenProducerTest {

    private lateinit var utstedtA1AivenProducer: UtstedtA1AivenProducer
    private lateinit var mockProducer: MockProducer<String, UtstedtA1Melding>

    @BeforeEach
    fun setUp() {
        mockProducer = MockProducer(true, StringSerializer(), JsonSerializer(OBJECT_MAPPER))
        val kafkaTemplate = KafkaTemplate<String, UtstedtA1Melding>(MockA1UtstedtMeldingProducerFactory())
        utstedtA1AivenProducer = UtstedtA1AivenProducer(kafkaTemplate, OBJECT_MAPPER, "topic")
    }

    @Test
    fun `produserMelding forventMelding`() {
        val sendtMelding = utstedtA1AivenProducer.produserMelding(lagMelding())
        sendtMelding shouldNotBe null
    }

    @Test
    fun `UtstedtA1Melding serialiserer datoer som ISO-strenger`() {
        val melding = lagMelding()
        val json = OBJECT_MAPPER.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.datoUtstedelse", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.fom", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.tom", "2025-09-01")
    }

    @Test
    fun `UtstedtA1Melding serialiserer artikkel som kode-streng via JsonValue`() {
        val melding = lagMelding()
        val json = OBJECT_MAPPER.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.artikkel", "11.3a")
    }

    @Test
    fun `UtstedtA1Melding serialiserer typeUtstedelse som enum-navn`() {
        val melding = lagMelding()
        val json = OBJECT_MAPPER.writeValueAsString(melding)

        json.shouldContainJsonKeyValue("$.typeUtstedelse", "FØRSTEGANG")
    }

    private inner class MockA1UtstedtMeldingProducerFactory : ProducerFactory<String, UtstedtA1Melding> {
        override fun createProducer(): Producer<String, UtstedtA1Melding> = mockProducer
        override fun createProducer(@Nullable txIdPrefix: String?): Producer<String, UtstedtA1Melding> = createProducer()
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

    companion object {
        private val OBJECT_MAPPER = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}
