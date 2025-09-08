package no.nav.melosys.statistikk.utstedt_a1.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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

    @BeforeEach
    fun setUp() {
        val kafkaTemplate = KafkaTemplate<String, UtstedtA1Melding>(MockA1UtstedtMeldingProducerFactory())
        utstedtA1AivenProducer = UtstedtA1AivenProducer(kafkaTemplate, OBJECT_MAPPER, "topic")
    }

    @Test
    fun `produserMelding forventMelding`() {
        val sendtMelding = utstedtA1AivenProducer.produserMelding(lagMelding())
        sendtMelding shouldNotBe null
    }

    private class MockA1UtstedtMeldingProducerFactory : ProducerFactory<String, UtstedtA1Melding> {
        override fun createProducer(): Producer<String, UtstedtA1Melding> =
            MockProducer(true, StringSerializer(), JsonSerializer(OBJECT_MAPPER))

        override fun createProducer(@Nullable txIdPrefix: String?): Producer<String, UtstedtA1Melding> = createProducer()
    }

    private fun lagMelding() = UtstedtA1Melding(
        "MEL-123",
        123L,
        "1234567898765",
        Lovvalgsbestemmelse.ART_11_3_a,
        Periode(LocalDate.now(), LocalDate.now().plusMonths(3L)),
        "SE",
        LocalDate.now(),
        A1TypeUtstedelse.FØRSTEGANG
    )

    companion object {
        private val OBJECT_MAPPER = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}
