package no.nav.melosys.integrasjonstest.saksflyt;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class KompTest extends ComponentTestBase {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void configLoad() throws Exception {
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(factory);
        MelosysEessiMelding melosysEessiMelding = objectMapper.readValue(this.getClass().getResourceAsStream("/sed.json"), MelosysEessiMelding.class);
        URI uri = this.getClass().getResource("/sed.json").toURI();
        String melo = Files.readString(Paths.get(uri));
        kafkaTemplate.send("privat-melosys-eessi-v1-local", melo);
    }
}
