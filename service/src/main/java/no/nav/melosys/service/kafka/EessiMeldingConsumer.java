package no.nav.melosys.service.kafka;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Profile("!itest")
@Service
public class EessiMeldingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiMeldingConsumer.class);

    private final ProsessinstansService prosessinstansService;

    public EessiMeldingConsumer(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    @KafkaListener(clientIdPrefix = "melosys-eessi-consumer", topics = "${kafka.topic.eessi}",
        containerFactory = "eessiMeldingListenerContainerFactory")
    public void mottaMelding(ConsumerRecord<String, MelosysEessiMelding> consumerRecord) {
        MelosysEessiMelding melding = consumerRecord.value();
        log.info("Mottatt ny melding fra eessi: {}", melding);

        try {
            prosessinstansService.opprettProsessinstansSedMottak(melding);
        } catch (Exception e) {
            log.error("Feil ved mottak av SED! ConsumerRecord.key: {}", consumerRecord.key(), e);
        }
    }
}
