package no.nav.melosys.service.vedtak;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.service.vedtak.data.FattetVedtakTestData;
import no.nav.melosys.service.vedtak.publisering.FattetVedtakProducer;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class FattetVedtakProducerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private static final String TOPIC = "topic";

    private FattetVedtakProducer fattetVedtakProducer;

    @BeforeEach
    void setUp() {
        KafkaTemplate<String, FattetVedtak> kafkaTemplate = new KafkaTemplate<>(new MockFattetVedtakProducerFactory());
        fattetVedtakProducer = new FattetVedtakProducer(kafkaTemplate, TOPIC, OBJECT_MAPPER);
    }

    @Test
    void produserMelding_validererRiktig() {
        FattetVedtak produsertMelding = fattetVedtakProducer.produserMelding(FattetVedtakTestData.lagFattetVedtak());
        assertThat(produsertMelding).isNotNull();
    }

    private static class MockFattetVedtakProducerFactory implements ProducerFactory<String, FattetVedtak> {
        @Override
        public Producer<String, FattetVedtak> createProducer() {
            return new MockProducer<>(true, new StringSerializer(), new JsonSerializer<>(new ObjectMapper()));
        }

        @Override
        public Producer<String, FattetVedtak> createProducer(@Nullable String txIdPrefix) {
            return createProducer();
        }
    }
}
