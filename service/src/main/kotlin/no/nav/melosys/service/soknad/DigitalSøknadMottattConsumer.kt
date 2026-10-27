package no.nav.melosys.service.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class DigitalSøknadMottattConsumer(
    private val prosessinstansService: ProsessinstansService
) {

    @KafkaListener(
        clientIdPrefix = "melosys-skjema-mottatt-consumer", //TODO: Endre topic til utsendt-arbeidstaker
        topics = ["\${kafka.aiven.skjema-mottatt.topic}"],
        containerFactory = "aivenSkjemaMottattContainerFactory"
    )
    fun mottaSkjemaMelding(
        consumerRecord: ConsumerRecord<String, SkjemaMottattMelding>
    ) {
        val melding = consumerRecord.value()
        log.info {
            "Mottatt skjema-melding med skjemaId=${melding.skjemaId}, recordOffset=${consumerRecord.offset()}, recordKey=${consumerRecord.key()}"
        }

        // Om søknaden gir ny sak eller festes på eksisterende sak avgjøres i sagaen. Relaterte søknader
        // havner i samme låsgruppe og behandles én om gangen, se ProsessinstansService.
        prosessinstansService.opprettProsessinstansDigitalSøknadMottatt(melding)
    }
}

