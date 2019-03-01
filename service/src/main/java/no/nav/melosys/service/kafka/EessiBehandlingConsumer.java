package no.nav.melosys.service.kafka;

import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EessiBehandlingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiBehandlingConsumer.class);

    @KafkaListener(clientIdPrefix = "melosys-eessi-consumer", topics = "privat-melosys-eessi-v1",
        containerFactory = "eessiBehandlingListenerContainerFactory")
    public void mottaBehandling(ConsumerRecord<String, MelosysEessiMelding> consumerRecord) {
        MelosysEessiMelding behandling = consumerRecord.value();
        log.info("Mottatt ny behandling fra eessi: {}", behandling.toString());
    }
}
