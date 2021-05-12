package no.nav.melosys.statistikk.utstedt_a1.integrasjon;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component
public class UtstedtA1Producer {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1Producer.class);
    private static final String A1_UTSTEDT_SCHEMA = "utstedt_a1/a1-utstedt-schema.json";

    private final KafkaTemplate<String, UtstedtA1Melding> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    @Autowired
    public UtstedtA1Producer(@Qualifier("utstedtA1") KafkaTemplate<String, UtstedtA1Melding> kafkaTemplate,
                             ObjectMapper objectMapper,
                             @Value("${kafka.a1-utstedt.topic}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public UtstedtA1Melding produserMelding(UtstedtA1Melding melding) {
        valider(melding);
        ListenableFuture<SendResult<String, UtstedtA1Melding>> future = kafkaTemplate.send(topicName, melding);

        try {
            SendResult<String, UtstedtA1Melding> res = future.get(15L, TimeUnit.SECONDS);
            log.info("Melding sendt på topic {} for behandling {}. Record.value: {}, offset: {}",
                topicName,
                res.getProducerRecord().value().getBehandlingId(),
                res.getProducerRecord().value(),
                res.getRecordMetadata().offset()
            );

            return res.getProducerRecord().value();
        } catch (ExecutionException | TimeoutException e) {
            throw new IntegrasjonException(
                "Kunne ikke sende melding om utstedt A1 for behandling " + melding.getBehandlingId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrasjonException("Avbrutt ved sending av melding om utstedt A1");
        }
    }

    private void valider(UtstedtA1Melding melding) {
        new JsonSchemaValidator(objectMapper).valider(melding, A1_UTSTEDT_SCHEMA);
    }
}
