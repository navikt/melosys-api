package no.nav.melosys.statistikk.utstedt_a1.integrasjon;

import java.time.LocalDate;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class UtstedtA1AivenProducerTest {

    private UtstedtA1AivenProducer utstedtA1AivenProducer;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @BeforeEach
    void setUp() {
        KafkaTemplate<String, UtstedtA1Melding> kafkaTemplate = new KafkaTemplate<>(new MockA1UtstedtMeldingProducerFactory());
        utstedtA1AivenProducer = new UtstedtA1AivenProducer(kafkaTemplate, OBJECT_MAPPER, "topic");
    }

    @Test
    void produserMelding_forventMelding() {
        UtstedtA1Melding sendtMelding = utstedtA1AivenProducer.produserMelding(lagMelding());
        assertThat(sendtMelding).isNotNull();
    }

    private static UtstedtA1Melding lagMelding() {
        return new UtstedtA1Melding(
            "MEL-123",
            123L,
            "1234567898765",
            Lovvalgsbestemmelse.ART_11_3_a,
            new Periode(LocalDate.now(), LocalDate.now().plusMonths(3L)),
            "SE",
            LocalDate.now(),
            A1TypeUtstedelse.FØRSTEGANG
        );
    }

    private static class MockA1UtstedtMeldingProducerFactory implements ProducerFactory<String, UtstedtA1Melding> {
        @Override
        public Producer<String, UtstedtA1Melding> createProducer() {
            return new MockProducer<>(true, new StringSerializer(), new JsonSerializer<>(OBJECT_MAPPER));
        }

        @Override
        public Producer<String, UtstedtA1Melding> createProducer(@Nullable String txIdPrefix) {
            return createProducer();
        }
    }
}
