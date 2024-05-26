package no.nav.melosys.service.avgift.aarsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
class SkattehendelserConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService
) {

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-skattehendelser-consumer",
        topics = ["\${kafka.aiven.skattehendelser.topic}"],
        containerFactory = "aivenSkattehendelserListenerContainerFactory"
    )
    fun lesSkattehendelser(consumerRecord: ConsumerRecord<String, Skattehendelse>) {
        val skattehendelseMelding = consumerRecord.value()
        prosessinstansService.opprettArsavregningsBehandlingProsessflyt(skattehendelseMelding)
    }

}
