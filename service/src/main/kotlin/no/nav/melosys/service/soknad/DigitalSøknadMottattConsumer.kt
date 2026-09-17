package no.nav.melosys.service.soknad

import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations.Companion.withKafkaCorrelationId
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger { }

/**
 * Kunstig pause mellom hver konsumerte melding.
 *
 * MELOSYS-8290: en race condition mellom saga-behandlingen av forrige melding (som kjører
 * async, AFTER_COMMIT) og mottak av neste melding kunne føre til at flere gyldige saker ble
 * opprettet for relaterte skjemaId-er samtidig. Da konsumenten stoppet opp av dette, hopet det
 * seg opp i størrelsesorden 200 uleste meldinger på topic-et før feilen ble oppdaget og rettet
 * manuelt. Denne pausen gir forrige melding bedre tid til å fullføre sin saga før neste melding
 * slippes gjennom. Dette er en midlertidig mitigering, ikke en fjerning av selve race'en — bør
 * fjernes når en mer robust løsning (f.eks. reell låsing) er på plass.
 */
private val KONSUMERING_DELAY: Duration = Duration.ofSeconds(20)

/**
 * NB: Thread.sleep() i en Kafka-lytter er normalt et anti-pattern (blokkerer konsument-tråden,
 * kan gi rebalansering/konsument kastet ut av gruppen). Her er det trygt fordi containeren for
 * dette topic-et er eksplisitt konfigurert (se `aivenSkjemaMottattContainerFactory` i KafkaConfig)
 * med:
 *  - kun 1 partisjon på topic-et (bekreftet i
 *    https://github.com/navikt/melosys-iac/blob/master/kafka/soknad-mottak.v1/prod-vars.yaml)
 *    → ingen andre partisjoner venter på denne tråden
 *  - MAX_POLL_RECORDS_CONFIG = 1 → én melding behandles fullt ferdig før neste hentes
 *  - default max.poll.interval.ms (5 min) → 20 sek sleep er langt innenfor grensen, ingen fare
 *    for at consumeren blir ansett som død og kastes ut av forbrukergruppen
 * Endres noen av disse forutsetningene (f.eks. concurrency > 1, flere partisjoner, eller kortere
 * max.poll.interval.ms), faller garantien om reelt 20 sek mellomrom mellom meldinger bort.
 */
private fun standardKonsumeringDelay() {
    Thread.sleep(KONSUMERING_DELAY.toMillis())
}

@Profile("!local-q1 & !local-q2")
@Service
class DigitalSøknadMottattConsumer(
    private val prosessinstansService: ProsessinstansService,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val konsumeringDelay: () -> Unit = ::standardKonsumeringDelay
) {

    @KafkaListener(
        clientIdPrefix = "melosys-skjema-mottatt-consumer", //TODO: Endre topic til utsendt-arbeidstaker
        topics = ["\${kafka.aiven.skjema-mottatt.topic}"],
        containerFactory = "aivenSkjemaMottattContainerFactory"
    )
    fun mottaSkjemaMelding(
        consumerRecord: ConsumerRecord<String, SkjemaMottattMelding>,
        @Headers headers: Map<String, ByteArray>
    ) = withKafkaCorrelationId(headers) {
        val melding = consumerRecord.value()
        log.info {
            "Mottatt skjema-melding med skjemaId=${melding.skjemaId}, recordOffset=${consumerRecord.offset()}, recordKey=${consumerRecord.key()}"
        }

        val alleIder = melding.relaterteSkjemaIder + melding.skjemaId

        val harEksisterendeSaksnummer = skjemaSakMappingService.harMappingMedGyldigSaksnummerForSkjemaId(alleIder)

        if (harEksisterendeSaksnummer) {
            log.info { "Fant eksisterende sak(er) for skjemaId ${melding.skjemaId}" }
            prosessinstansService.opprettProsessinstansEksisterendeDigitalSøknad(melding)
        } else {
            prosessinstansService.opprettProsessinstansMelosysDigitalSøknadMottatt(melding)
        }

        konsumeringDelay()
    }
}

