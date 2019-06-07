package no.nav.melosys.service.kafka;

import no.nav.melosys.service.eessi.EessiMottakService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EessiMeldingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiMeldingConsumer.class);

    private final EessiMottakService eessiMottakService;

    public EessiMeldingConsumer(EessiMottakService eessiMottakService) {
        this.eessiMottakService = eessiMottakService;
    }

    @KafkaListener(clientIdPrefix = "melosys-eessi-consumer", topics = "${kafka.topic.eessi}",
        containerFactory = "eessiMeldingListenerContainerFactory")
    public void mottaMelding(ConsumerRecord<String, MelosysEessiMelding> consumerRecord) {
        MelosysEessiMelding melding = consumerRecord.value();
        log.info("Mottatt ny melding fra eessi: {}", melding);

        try {
            eessiMottakService.behandleMottattMelding(melding);
        } catch (Exception e) {
            log.error("Feil ved mottak av SED! ConsumerRecord.key: {}", consumerRecord.key(), e);
        }
    }
}
