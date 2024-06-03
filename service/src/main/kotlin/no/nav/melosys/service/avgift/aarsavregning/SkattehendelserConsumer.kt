package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class SkattehendelserConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val unleash: Unleash
) {

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-skattehendelser-consumer",
        topics = ["\${kafka.aiven.skattehendelser.topic}"],
        containerFactory = "aivenSkattehendelserListenerContainerFactory"
    )
    fun lesSkattehendelser(consumerRecord: ConsumerRecord<String, Skattehendelse>) {
        if (unleash.isEnabled(ToggleName.MELOSYS_SKATTEHENDELSE_CONSUMER)) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(consumerRecord.value())
        } else {
            log.info { "Skattehendelsemelding med key: ${consumerRecord.key()}" }
        }
    }

}
