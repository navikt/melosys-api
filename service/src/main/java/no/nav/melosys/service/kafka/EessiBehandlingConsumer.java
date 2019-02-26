package no.nav.melosys.service.kafka;

import no.nav.melosys.eessi.avro.MelosysEessiBehandling;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EessiBehandlingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiBehandlingConsumer.class);

    @KafkaListener(clientIdPrefix = "melosys-eessi-eessiBehandling", topics = "privat-melosys-eessi-behandling-v1",
        containerFactory = "eessiBehandlingListenerContainerFactory")
    public void mottaBehandling(ConsumerRecord<String, MelosysEessiBehandling> consumerRecord) {
        MelosysEessiBehandling behandling = consumerRecord.value();
        log.info("Mottatt ny behandling fra eessi: {}", behandling.toString());
    }
}
