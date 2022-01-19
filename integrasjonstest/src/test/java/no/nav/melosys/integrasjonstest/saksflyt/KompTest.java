package no.nav.melosys.integrasjonstest.saksflyt;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.melosysmock.testdata.OpprettJfrOppgaveRequest;
import no.nav.melosys.melosysmock.testdata.TestDataGenerator;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class KompTest extends ComponentTestBase {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestDataGenerator testDataGenerator;

    @Test
    void configLoad() throws Exception {
        Thread.sleep(5_000);
        OpprettJfrOppgaveRequest request = new OpprettJfrOppgaveRequest("Z123456", "439646307");
        testDataGenerator.lagJournalføringsoppgave(request);

        DefaultKafkaProducerFactory<String, MelosysEessiMelding> factory = new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafkaBroker), new StringSerializer(), new JsonSerializer<>(objectMapper));
        KafkaTemplate<String, MelosysEessiMelding> kafkaTemplate = new KafkaTemplate<>(factory);
        MelosysEessiMelding melosysEessiMelding = objectMapper.readValue(this.getClass().getResourceAsStream("/sed.json"), MelosysEessiMelding.class);
        kafkaTemplate.send("privat-melosys-eessi-v1-local", melosysEessiMelding);

        Thread.sleep(5_000);
    }
}
