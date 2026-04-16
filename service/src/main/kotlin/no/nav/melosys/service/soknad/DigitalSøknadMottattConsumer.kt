package no.nav.melosys.service.soknad

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations.Companion.withKafkaCorrelationId
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class DigitalSøknadMottattConsumer(
    private val unleash: Unleash,
    private val prosessinstansService: ProsessinstansService,
    private val skjemaSakMappingService: SkjemaSakMappingService
) {

    @KafkaListener(
        clientIdPrefix = "melosys-skjema-mottatt-consumer",
        topics = ["\${kafka.aiven.skjema-mottatt.topic}"],
        containerFactory = "aivenSkjemaMottattContainerFactory"
    )
    fun mottaSkjemaMelding(
        consumerRecord: ConsumerRecord<String, SkjemaMottattMelding>,
        @Headers headers: Map<String, ByteArray>
    ) = withKafkaCorrelationId(headers) {
        val melding = consumerRecord.value()
        log.info { "Mottatt skjema-melding med skjemaId: ${melding.skjemaId}" }

        if (unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER)) { //TODO: Er toggle nødvendig?
            val alleIder = melding.relaterteSkjemaIder + melding.skjemaId
            val eksisterendeSaksnummer = skjemaSakMappingService.finnSaksnummerForGyldigSak(alleIder)

            if (eksisterendeSaksnummer != null) {
                log.info { "Fant eksisterende sak $eksisterendeSaksnummer for skjemaId ${melding.skjemaId}" }
                prosessinstansService.opprettProsessinstansEksisterendeDigitalSøknad(melding, eksisterendeSaksnummer)
            } else {
                prosessinstansService.opprettProsessinstansMelosysDigitalSøknadMottatt(melding)
            }
        }
    }
}
