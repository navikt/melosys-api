package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.kontroll.ProsessinstansAdminService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.UUID

/**
 * Relaterte digitale søknader som mottas tett på hverandre skal havne på samme sak (MELOSYS-8151).
 *
 * Den senere søknaden arver låsgruppen til den relaterte søknaden og settes PÅ_VENT til den første
 * prosessen er ferdig. Først da avklares sak, og mappingen fra den første søknaden finnes.
 */
class DigitalSøknadSamtidigMottakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansAdminService: ProsessinstansAdminService,
    @Autowired private val fagsakRepository: FagsakRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150" // KARAFFEL TRIVIELL fra PersonRepo i melosys-mock

    @Test
    fun `relatert søknad som mottas mens første prosess kjører venter og havner på samme sak`() {
        val førsteSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val førsteSkjemaId = førsteSøknadsdata.skjema.id
        // Forsinkelsen holder første prosess aktiv mens den andre mottas
        stubSkjemaEndpoints(førsteSkjemaId, førsteSøknadsdata, forsinkelseMs = 3000)

        val andreSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val andreSkjemaId = andreSøknadsdata.skjema.id
        stubSkjemaEndpoints(andreSkjemaId, andreSøknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(førsteSkjemaId))
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(andreSkjemaId, listOf(førsteSkjemaId)))

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(andreSkjemaId)?.status == ProsessStatus.PÅ_VENT
        }
        fagsakRepository.count() shouldBe 0

        ventTilFerdig(førsteSkjemaId, andreSkjemaId)

        verifiserSammeSak(førsteSkjemaId, andreSkjemaId)
    }

    @Test
    fun `relatert søknad venter på feilet første prosess og havner på samme sak etter restart`() {
        val førsteSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val førsteSkjemaId = førsteSøknadsdata.skjema.id
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$førsteSkjemaId/data"))
                .willReturn(WireMock.aResponse().withStatus(500))
        )

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(førsteSkjemaId))
        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(førsteSkjemaId)?.status == ProsessStatus.FEILET
        }

        val andreSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val andreSkjemaId = andreSøknadsdata.skjema.id
        stubSkjemaEndpoints(andreSkjemaId, andreSøknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(andreSkjemaId, listOf(førsteSkjemaId)))

        // Feilet prosess holder fortsatt låsgruppen, så den andre skal ikke opprette egen sak
        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(andreSkjemaId)?.status == ProsessStatus.PÅ_VENT
        }
        await.during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).until {
            prosessinstansRepository.finnDigitalSøknadProsess(andreSkjemaId)?.status == ProsessStatus.PÅ_VENT
        }
        fagsakRepository.count() shouldBe 0

        stubSkjemaEndpoints(førsteSkjemaId, førsteSøknadsdata)
        val førsteProsessId = prosessinstansRepository.finnDigitalSøknadProsess(førsteSkjemaId).shouldNotBeNull().id!!
        prosessinstansAdminService.restartProsessinstanser(listOf(førsteProsessId))

        ventTilFerdig(førsteSkjemaId, andreSkjemaId)

        verifiserSammeSak(førsteSkjemaId, andreSkjemaId)
    }

    private fun ventTilFerdig(vararg skjemaIder: UUID) {
        await.atMost(Duration.ofSeconds(20)).until {
            skjemaIder.all { prosessinstansRepository.finnDigitalSøknadProsess(it)?.status == ProsessStatus.FERDIG }
        }
    }

    private fun verifiserSammeSak(førsteSkjemaId: UUID, andreSkjemaId: UUID) {
        val første = prosessinstansRepository.finnDigitalSøknadProsess(førsteSkjemaId).shouldNotBeNull()
        val andre = prosessinstansRepository.finnDigitalSøknadProsess(andreSkjemaId).shouldNotBeNull()

        andre.låsReferanse shouldBe "${førsteSkjemaId}_$andreSkjemaId"
        første.hentData<Boolean>(ProsessDataKey.DIGITAL_SØKNAD_NY_SAK) shouldBe true
        andre.hentData<Boolean>(ProsessDataKey.DIGITAL_SØKNAD_NY_SAK) shouldBe false
        andre.behandling.shouldNotBeNull().fagsak.saksnummer shouldBe
            første.behandling.shouldNotBeNull().fagsak.saksnummer
        fagsakRepository.count() shouldBe 1
    }

    private fun stubSkjemaEndpoints(
        skjemaId: UUID,
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto,
        forsinkelseMs: Int = 0,
    ) {
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withFixedDelay(forsinkelseMs)
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
