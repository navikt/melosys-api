package no.nav.melosys.service.eessi.kafka;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EessiMeldingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiMeldingConsumer.class);

    private final ProsessinstansService prosessinstansService;

    public EessiMeldingConsumer(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    @KafkaListener(clientIdPrefix = "aiven-melosys-eessi-consumer", topics = "${kafka.aiven.eessi.topic}",
        containerFactory = "aivenEessiMeldingListenerContainerFactory")
    public void mottaMeldingAiven(ConsumerRecord<String, MelosysEessiMelding> consumerRecord) {
        MelosysEessiMelding melding = consumerRecord.value();
        log.info("Mottatt ny melding fra eessi(aiven): {}", melding);

        try {
            prosessinstansService.opprettProsessinstansSedMottak(melding);
        } catch (Exception e) {
            log.error("Feil ved mottak av SED(aiven)! ConsumerRecord.key: {}", consumerRecord.key(), e);
        }
    }
}
