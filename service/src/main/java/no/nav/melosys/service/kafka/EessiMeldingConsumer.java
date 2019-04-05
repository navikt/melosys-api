package no.nav.melosys.service.kafka;

import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EessiMeldingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiMeldingConsumer.class);

    @KafkaListener(clientIdPrefix = "melosys-eessi-consumer", topics = "privat-melosys-eessi-v1",
        containerFactory = "eessiMeldingListenerContainerFactory")
    public void mottaMelding(ConsumerRecord<String, MelosysEessiMelding> consumerRecord) {
        MelosysEessiMelding melding = consumerRecord.value();
        log.info("Mottatt ny melding fra eessi: {}", melding);
    }
}
