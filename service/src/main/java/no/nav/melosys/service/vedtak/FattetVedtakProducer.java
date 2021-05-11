package no.nav.melosys.service.vedtak;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.service.vedtak.dto.FattetVedtak;
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
public class FattetVedtakProducer {
    private static final Logger log = LoggerFactory.getLogger(FattetVedtakProducer.class);
    private static final String FATTET_VEDTAK_SCHEMA = "fattet-vedtak-schema.json";

    private final KafkaTemplate<String, FattetVedtak> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    @Autowired
    public FattetVedtakProducer(@Qualifier("fattetVedtak") KafkaTemplate<String, FattetVedtak> kafkaTemplate,
                                ObjectMapper objectMapper,
                                @Value("${kafka.fattet-vedtak.topic}")String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public void publiserMelding(FattetVedtak fattetVedtak) {
        validerMelding(fattetVedtak);
        ListenableFuture<SendResult<String, FattetVedtak>> sendt = kafkaTemplate.send(topicName, fattetVedtak);

        try {
            SendResult<String, FattetVedtak> res = sendt.get(15L, TimeUnit.SECONDS);
            log.info("Melding publisert på topic {} for behandling {}. Record.value: {}, offset: {}",
                topicName,
                res.getProducerRecord().value().sak().behandlingId(),
                res.getProducerRecord().value(),
                res.getRecordMetadata().offset()
            );
        } catch (ExecutionException | TimeoutException e) {
            throw new IntegrasjonException("Kunne ikke publisere melding om fattet vedtak for behandling " + fattetVedtak.sak().behandlingId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrasjonException("Avbrutt ved publisering av fattet vedtak");
        }
    }

    private void validerMelding(FattetVedtak fattetVedtak) {
        new JsonSchemaValidator(objectMapper).valider(fattetVedtak, FATTET_VEDTAK_SCHEMA);
    }
}
