package no.nav.melosys.service.vedtak.publisering;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component
public class FattetVedtakProducer {

    private static final Logger log = LoggerFactory.getLogger(FattetVedtakProducer.class);
    private static final String FATTET_VEDTAK_SCHEMA = "fattet-vedtak-schema.json";

    private final KafkaTemplate<String, FattetVedtak> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public FattetVedtakProducer(@Qualifier("fattetVedtak") KafkaTemplate<String, FattetVedtak> kafkaTemplate,
                                @Value("${kafka.fattetvedtak.topic}") String topicName,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public FattetVedtak produserMelding(FattetVedtak fattetVedtak) {
        valider(fattetVedtak);
        ListenableFuture<SendResult<String, FattetVedtak>> future = kafkaTemplate.send(topicName, fattetVedtak);

        try {
            SendResult<String, FattetVedtak> res = future.get(15L, TimeUnit.SECONDS);
            log.info("Melding sendt på topic {} for sak {}. Offset: {}",
                topicName, res.getProducerRecord().value().sak().saksnummer(), res.getRecordMetadata().offset()
            );
            return res.getProducerRecord().value();
        } catch (ExecutionException | TimeoutException e) {
            throw new IntegrasjonException("Kunne ikke sende melding om fattet vedtak for sak " + fattetVedtak.sak().saksnummer(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrasjonException("Avbrutt ved sending av melding om fattet vedtak");
        }
    }

    private void valider(FattetVedtak fattetVedtak) {
        new JsonSchemaValidator(objectMapper).valider(fattetVedtak, FATTET_VEDTAK_SCHEMA);
    }
}
