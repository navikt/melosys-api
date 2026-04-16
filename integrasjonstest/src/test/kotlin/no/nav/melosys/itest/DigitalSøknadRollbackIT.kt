package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkSpyBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

/**
 * Integrasjonstest som verifiserer at @Transactional(REQUIRES_NEW) på StegBehandler.utfør()
 * faktisk gir rollback av alle databaseendringer innenfor et steg ved feil.
 *
 * Bruker @MockkSpyBean for MottatteOpplysningerService for å trigge en exception ETTER at
 * fagsakService.nyFagsakOgBehandling() allerede har skrevet fagsak+behandling til databasen.
 *
 * Har egen testklasse fordi @MockkSpyBean endrer Spring-konteksten og ikke bør påvirke andre tester.
 */
class DigitalSøknadRollbackIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
) : MockServerTestBaseWithProsessManager() {

    @MockkSpyBean
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"

    @BeforeEach
    fun setupMocks() {
        fakeUnleash.enable(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER)
    }

    @Test
    fun `rollback ved feil i OPPRETT_SAK_OG_BEHANDLING_SØKNAD - fagsak og behandling rulles tilbake`() {
        val testFnr = "30056928150"

        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
        }
        val skjemaId = søknadsdata.skjema.id

        // Konfigurer spy til å kaste exception ved lagring av mottatte opplysninger.
        // På dette tidspunktet har fagsakService.nyFagsakOgBehandling() allerede skrevet
        // fagsak+behandling til databasen innenfor stegets REQUIRES_NEW-transaksjon.
        every {
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any())
        } throws RuntimeException("Simulert feil for å teste rollback")

        // Stub melosys-skjema-api endpoint for søknadsdata
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(søknadsdata))
                )
        )

        // Verifiser at databasen er tom før sagaen starter
        fagsakRepository.count() shouldBe 0

        val melding = SkjemaMottattMelding(skjemaId)

        // Send Kafka-melding — starter sagaen.
        // Steg 1 (HENT_SØKNADSDATA) lykkes.
        // Steg 2 (OPPRETT_SAK_OG_BEHANDLING_SØKNAD) oppretter fagsak+behandling,
        // men kaster exception ved lagring av mottatte opplysninger → hele stegets transaksjon rulles tilbake.
        kafkaTemplate.send(kafkaTopic, melding)

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FEILET
        }

        val prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString()).single()

        // Prosessinstans skal ha FEILET status med hendelse på riktig steg
        prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
        prosessinstans.status shouldBe ProsessStatus.FEILET
        prosessinstans.hendelser.shouldHaveSize(1)
        prosessinstans.hendelser.first().steg shouldBe ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD

        // KJERNEASSERTION: Verifiser at @Transactional(REQUIRES_NEW) på StegBehandler.utfør()
        // har rullet tilbake fagsak+behandling som ble opprettet av fagsakService.nyFagsakOgBehandling().
        // Databasen skal være tom — ingen fagsaker skal finnes.
        fagsakRepository.count() shouldBe 0
    }
}
