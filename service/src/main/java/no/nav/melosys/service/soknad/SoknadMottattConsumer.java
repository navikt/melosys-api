package no.nav.melosys.service.soknad;

import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SoknadMottattConsumer {
    private static final Logger log = LoggerFactory.getLogger(SoknadMottattConsumer.class);

    private final ProsessinstansService prosessinstansService;

    public SoknadMottattConsumer(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    public void mottaAivenMelding(ConsumerRecord<String, SoknadMottatt> consumerRecord) {
        SoknadMottatt melding = consumerRecord.value();
        log.info("Mottatt ny melding fra altinn via aiven: {}", melding);

        try {
            prosessinstansService.opprettProsessinstansSøknadMottatt(melding);
        } catch (Exception e) {
            log.error("Feil ved mottak av søknad fra altinn via aiven! SoknadID: {} ConsumerRecord.key: {}",
                consumerRecord.value().getSoknadID(), consumerRecord.key(), e);
        }
    }
}
