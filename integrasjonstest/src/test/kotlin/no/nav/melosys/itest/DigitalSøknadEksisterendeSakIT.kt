package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
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
 * Integrasjonstest for MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten.
 *
 * Scenariet:
 * 1. Send en første digital søknad → oppretter fagsak, behandling (FØRSTEGANG, OPPRETTET) og mapping
 * 2. Send en ny søknad med relaterteSkjemaIder som peker til den første
 * 3. Consumer finner eksisterende sak via mapping og starter eksisterende-sak-flyt
 * 4. HåndterEksisterendeSakSøknad finner åpen FØRSTEGANG-behandling i OPPRETTET-status
 *    og oppdaterer mottatte opplysninger (uten statusendring, fordi OPPRETTET ikke trigger reset)
 *
 * Dekker også at en søknad som er relatert til flere åpne saker havner på den sist opprettede.
 */
class DigitalSøknadEksisterendeSakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150" // KARAFFEL TRIVIELL fra PersonRepo i melosys-mock

    @Test
    fun `mottak av digital søknad på eksisterende sak gjenbruker åpen behandling`() {
        // --- Steg 1: Send første søknad som oppretter fagsak ---
        val førsteSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            orgnr = "FØRSTE-ORG"
        }
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
        // Bytt orgnr for å verifisere at oppdaterMottatteOpplysningerFraSøknad overskriver
        val andreSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            orgnr = "ANDRE-ORG"
        }
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
        andreProsessinstans.sistFullførtSteg shouldBe ProsessSteg.SEND_FORVALTNINGSMELDING
        andreProsessinstans.hendelser.shouldHaveSize(0)

        // Verifiser at skjemaId ble lagret
        val lagretSkjemaId = andreProsessinstans.hentData<UUID>(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID)
        lagretSkjemaId shouldBe andreSkjemaId

        // Verifiser søknadsdata ble hentet og lagret
        val søknadsdata = andreProsessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        søknadsdata.skjema.fnr shouldBe testFnr

        // Verifiser behandling: gjenbrukt FØRSTEGANG-behandling på SAMME fagsak
        val behandling = andreProsessinstans.behandling.shouldNotBeNull()
        behandling.fagsak.saksnummer shouldBe saksnummer
        behandling.type shouldBe Behandlingstyper.FØRSTEGANG
        behandling.tema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER

        // Verifiser at mottatte opplysninger ble oppdatert med orgnr fra siste søknad
        val mottatteOpplysninger = behandling.mottatteOpplysninger.shouldNotBeNull()
        val soeknad = mottatteOpplysninger.mottatteOpplysningerData as Soeknad
        soeknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("ANDRE-ORG")

        // Verifiser at forvaltningsmelding ble bestilt for begge søknader (default skjemadel = ARBEIDSTAKERS_DEL → BRUKER)
        await.atMost(Duration.ofSeconds(5)).until {
            prosessinstansRepository.findAll().count { it.type == ProsessType.OPPRETT_OG_DISTRIBUER_BREV } == 2
        }
        val brevProsessinstanser = prosessinstansRepository.findAll()
            .filter { it.type == ProsessType.OPPRETT_OG_DISTRIBUER_BREV }
        brevProsessinstanser shouldHaveSize 2
        brevProsessinstanser.forEach { brev ->
            brev.hentData<Mottakerroller>(ProsessDataKey.MOTTAKER) shouldBe Mottakerroller.BRUKER
            brev.hentData<DokgenBrevbestilling>(ProsessDataKey.BREVBESTILLING)
                .produserbartdokument shouldBe Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
        }
    }

    @Test
    fun `mottak av digital søknad som er relatert til flere åpne saker journalføres på sist opprettede sak`() {
        // To innsendinger uten relasjon → to separate åpne saker for samme person
        val (eldsteSkjemaId, eldsteSaksnummer) = sendSøknadPåNySak()
        val (nyesteSkjemaId, nyesteSaksnummer) = sendSøknadPåNySak()
        eldsteSaksnummer shouldNotBe nyesteSaksnummer

        // Ny innsending med periode som overlapper begge → relatert til skjema i begge sakene
        val relaterteSkjemaIder = listOf(eldsteSkjemaId, nyesteSkjemaId)

        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val skjemaId = søknadsdata.skjema.id
        stubSkjemaEndpoints(skjemaId, søknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId, relaterteSkjemaIder))

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        val prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString()).single()
        prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
        prosessinstans.hendelser.shouldHaveSize(0)
        prosessinstans.behandling.shouldNotBeNull().fagsak.saksnummer shouldBe nyesteSaksnummer
    }

    private fun sendSøknadPåNySak(): Pair<UUID, String> {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val skjemaId = søknadsdata.skjema.id
        stubSkjemaEndpoints(skjemaId, søknadsdata)

        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(skjemaId))

        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        val prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString()).single()
        prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
        return skjemaId to prosessinstans.behandling.shouldNotBeNull().fagsak.saksnummer
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
