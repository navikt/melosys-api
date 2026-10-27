package no.nav.melosys.itest

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.UUID

/**
 * Verifiserer dedupen på skjemaId (slutten av låsreferansen, på tvers av alle digital-søknad-typene):
 * redeliveryen oppretter IKKE en duplikat-prosess, heller ikke når første levering ble behandlet av en
 * utgått type med gammelt låsformat. Dermed unngås både dobbelt forvaltningsmelding og 409 fra journalføringen.
 */
class DigitalSøknadRedeliveryDedupIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150" // KARAFFEL TRIVIELL fra PersonRepo i melosys-mock

    // Fanger dedup-WARN fra ProsessinstansService, så vi deterministisk vet at redeliveryen ble konsumert.
    private val dedupLogWatcher = ListAppender<ILoggingEvent>()
    private val prosessinstansServiceLogger =
        LoggerFactory.getLogger(ProsessinstansService::class.java) as Logger

    @AfterEach
    fun detachLogWatcher() {
        prosessinstansServiceLogger.detachAppender(dedupLogWatcher)
    }

    @Test
    fun `redelivery av samme skjema oppretter ikke duplikat-prosess`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            orgnr = "REDELIVERY-ORG"
        }
        val skjemaId = søknadsdata.skjema.id
        stubSkjemaEndpoints(skjemaId, søknadsdata)

        // --- Levering 1: oppretter sak, behandling og skjema→sak-mapping ---
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(skjemaId)?.status == ProsessStatus.FERDIG
        }

        // Levering 1 sender ett forvaltningsmelding-brev (default skjemadel ARBEIDSTAKERS_DEL → BRUKER)
        await.atMost(Duration.ofSeconds(5)).until {
            prosessinstansRepository.findAll().any { it.type == ProsessType.OPPRETT_OG_DISTRIBUER_BREV }
        }

        // Lytt etter dedup-loggen før redeliveryen sendes
        dedupLogWatcher.start()
        prosessinstansServiceLogger.addAppender(dedupLogWatcher)

        // --- Levering 2: SAMME skjemaId (Kafka-redelivery). Saken finnes nå ---
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId))

        ventPåDedupLogg(skjemaId)

        // Ingen duplikat-prosess: fortsatt kun én prosess for dette skjemaet
        prosessinstansRepository.findAll()
            .filter { it.type.erDigitalSøknadMottak() } shouldHaveSize 1

        // Kun ett forvaltningsmelding-brev — ikke dobbelt
        prosessinstansRepository.findAll()
            .filter { it.type == ProsessType.OPPRETT_OG_DISTRIBUER_BREV } shouldHaveSize 1
    }

    @Test
    fun `redelivery av skjema behandlet med utgått type og gammelt låsformat oppretter ikke duplikat`() {
        val skjemaId = UUID.randomUUID()
        lagreFerdigProsessMedGammeltFormat(skjemaId)

        dedupLogWatcher.start()
        prosessinstansServiceLogger.addAppender(dedupLogWatcher)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId))

        ventPåDedupLogg(skjemaId)
        prosessinstansRepository.findAll()
            .filter { it.type.erDigitalSøknadMottak() } shouldHaveSize 1
    }

    @Test
    fun `ny søknad relatert til skjema med gammelt låsformat arver skjemaId-en som låsgruppe`() {
        val gammelSkjemaId = UUID.randomUUID()
        lagreFerdigProsessMedGammeltFormat(gammelSkjemaId)

        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val skjemaId = søknadsdata.skjema.id
        stubSkjemaEndpoints(skjemaId, søknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId, listOf(gammelSkjemaId)))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(skjemaId)?.status == ProsessStatus.FERDIG
        }
        prosessinstansRepository.finnDigitalSøknadProsess(skjemaId)!!.låsReferanse shouldBe "${gammelSkjemaId}_$skjemaId"
    }

    private fun lagreFerdigProsessMedGammeltFormat(skjemaId: UUID) {
        // Lagres direkte i repoet, så ingen saga startes
        prosessinstansRepository.save(
            Prosessinstans.forTest {
                id = null // la JPA generere id, ellers tolkes save som merge av en eksisterende rad
                type = ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
                status = ProsessStatus.FERDIG
                låsReferanse = skjemaId.toString()
            }
        )
    }

    private fun ventPåDedupLogg(skjemaId: UUID) {
        await.atMost(Duration.ofSeconds(15)).until {
            dedupLogWatcher.list.toList().any {
                it.formattedMessage.contains("hopper over redelivery") && it.formattedMessage.contains(skjemaId.toString())
            }
        }
    }

    private fun stubSkjemaEndpoints(skjemaId: UUID, søknadsdata: UtsendtArbeidstakerSkjemaM2MDto) {
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(søknadsdata))
                )
        )
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/pdf"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody("PDF content".toByteArray())
                )
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/saksnummer"))
                .willReturn(WireMock.aResponse().withStatus(204))
        )
    }
}
