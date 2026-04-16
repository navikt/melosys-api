package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

/**
 * Integrasjonstest for MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten.
 *
 * Scenariet:
 * 1. Send en første digital søknad → oppretter fagsak, behandling (FØRSTEGANG, OPPRETTET) og mapping
 * 2. Send en ny søknad med relaterteSkjemaIder som peker til den første
 * 3. Consumer finner eksisterende sak via mapping og starter eksisterende-sak-flyt
 * 4. HåndterEksisterendeSakSøknad finner åpen FØRSTEGANG-behandling i OPPRETTET-status
 *    og oppdaterer mottatte opplysninger (uten statusendring, fordi OPPRETTET ikke trigger reset)
 */
class DigitalSøknadEksisterendeSakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150" // KARAFFEL TRIVIELL fra PersonRepo i melosys-mock

    @BeforeEach
    fun setupMocks() {
        fakeUnleash.enable(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER)
    }

    @Test
    fun `mottak av digital søknad på eksisterende sak gjenbruker åpen behandling`() {
        // --- Steg 1: Send første søknad som oppretter fagsak ---
        val førsteSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val førsteSkjemaId = førsteSøknadsdata.skjema.id

        stubSkjemaEndpoints(førsteSkjemaId, førsteSøknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(førsteSkjemaId))

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(førsteSkjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        // Hent saksnummer fra den opprettede fagsaken
        val førsteProsessinstans = prosessinstansRepository
            .findAllByLåsReferanseStartingWith(førsteSkjemaId.toString()).single()
        førsteProsessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
        val saksnummer = førsteProsessinstans.behandling.shouldNotBeNull().fagsak.saksnummer

        // --- Steg 2: Send ny søknad med referanse til den første ---
        val andreSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val andreSkjemaId = andreSøknadsdata.skjema.id

        stubSkjemaEndpoints(andreSkjemaId, andreSøknadsdata)

        // relaterteSkjemaIder peker til det første skjemaet → consumer finner eksisterende sak
        val andreMelding = SkjemaMottattMelding(andreSkjemaId, listOf(førsteSkjemaId))
        kafkaTemplate.send(kafkaTopic, andreMelding)

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(andreSkjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        // --- Steg 3: Verifiser eksisterende-sak-flyten ---
        val andreProsessinstans = prosessinstansRepository
            .findAllByLåsReferanseStartingWith(andreSkjemaId.toString()).single()

        andreProsessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
        andreProsessinstans.status shouldBe ProsessStatus.FERDIG
        andreProsessinstans.sistFullførtSteg shouldBe ProsessSteg.SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API
        andreProsessinstans.hendelser.shouldHaveSize(0)

        // Verifiser at meldingdata ble lagret
        val mottattMelding = andreProsessinstans.hentData<SkjemaMottattMelding>(ProsessDataKey.DIGITAL_SØKNAD_MOTTATT_MELDING)
        mottattMelding.skjemaId shouldBe andreSkjemaId

        // Verifiser søknadsdata ble hentet og lagret
        val søknadsdata = andreProsessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        søknadsdata.skjema.fnr shouldBe testFnr

        // Verifiser behandling: gjenbrukt FØRSTEGANG-behandling på SAMME fagsak
        val behandling = andreProsessinstans.behandling.shouldNotBeNull()
        behandling.fagsak.saksnummer shouldBe saksnummer
        behandling.type shouldBe Behandlingstyper.FØRSTEGANG
        behandling.tema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER

        // Verifiser at mottatte opplysninger ble oppdatert
        behandling.mottatteOpplysninger.shouldNotBeNull()
    }

    private fun stubSkjemaEndpoints(skjemaId: java.util.UUID, søknadsdata: UtsendtArbeidstakerSkjemaM2MDto) {
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
