package no.nav.melosys.service.eessi.kafka;

import java.util.Map;
import java.util.UUID;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import static no.nav.melosys.config.MDCOperations.CORRELATION_ID;
import static no.nav.melosys.config.MDCOperations.putToMDC;

@Service
@Profile("!local-q1 & !local-q2")
public class EessiMeldingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiMeldingConsumer.class);

    private final ProsessinstansService prosessinstansService;

    public EessiMeldingConsumer(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    @KafkaListener(clientIdPrefix = "aiven-melosys-eessi-consumer", topics = "${kafka.aiven.eessi.topic}",
        containerFactory = "aivenEessiMeldingListenerContainerFactory")
    public void mottaMeldingAiven(ConsumerRecord<String, MelosysEessiMelding> consumerRecord, @Headers Map<String, byte[]> header) {
        putToMDC(CORRELATION_ID, getCorrelationId(header));
        MelosysEessiMelding melding = consumerRecord.value();
        log.info("Mottatt ny melding fra eessi(aiven): {}", melding);

        try {
            prosessinstansService.opprettProsessinstansSedMottak(melding);
        } catch (Exception e) {
            log.error("Feil ved mottak av SED(aiven)! ConsumerRecord.key: {}", consumerRecord.key(), e);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    private String getCorrelationId(Map<String, byte[]> header) {
        byte[] bytes = header.get(CORRELATION_ID);
        if (bytes != null && bytes.length > 0) {
            return new String(bytes);
        } else {
            return UUID.randomUUID().toString();
        }
    }
}
