package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate

import java.time.Duration
import java.util.UUID

class DigitalSøknadMottakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"

    @BeforeEach
    fun setupMocks() {
        // Enable feature toggle for skjema consumer
        fakeUnleash.enable(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER)
    }

    @Test
    fun `mottak av digital søknad starter saga og henter søknadsdata fra melosys-skjema-api`() {
        val skjemaId = UUID.randomUUID()
        // Bruker fnr fra PersonRepo i melosys-mock (KARAFFEL TRIVIELL)
        val testFnr = "30056928150"

        val forventetSøknadsdata = UtsendtArbeidstakerM2MSkjemaData(
            skjemaer = listOf(
                UtsendtArbeidstakerSkjemaDto(
                    id = skjemaId,
                    status = SkjemaStatus.SENDT,
                    fnr = testFnr,
                    orgnr = "123456789",
                    metadata = DegSelvMetadata(
                        skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                        arbeidsgiverNavn = "Test AS",
                        juridiskEnhetOrgnr = "987654321"
                    ),
                    data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
                )
            ),
            referanseId = "MEL-$skjemaId"
        )

        // Stub melosys-skjema-api endpoint
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(forventetSøknadsdata))
                )
        )

        val melding = SkjemaMottattMelding(skjemaId)

        // Send Kafka message
        kafkaTemplate.send(kafkaTopic, melding)

        // Wait for saga to fail at step 3 (expected - step not implemented yet)
        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FEILET
        }

        // Fetch and verify prosessinstans state
        val prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString()).single()

        // Basic prosessinstans info
        prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
        prosessinstans.status shouldBe ProsessStatus.FEILET
        prosessinstans.låsReferanse shouldBe skjemaId.toString()

        // Step progression - step 1 and 2 completed, failed at step 3
        prosessinstans.sistFullførtSteg shouldBe ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_SØKNAD

        // Error hendelse - verify we failed at the expected step
        prosessinstans.hendelser.shouldHaveSize(1)
        prosessinstans.hendelser.first().steg shouldBe ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD
        prosessinstans.hendelser.first().type shouldBe "NoSuchElementException"

        // Verify data stored by consumer (SØKNAD_MOTTATT_MELDING)
        val mottattMelding = prosessinstans.hentData<SkjemaMottattMelding>(ProsessDataKey.SØKNAD_MOTTATT_MELDING)
        mottattMelding shouldBe melding

        // Verify data stored by step 1 (SØKNADSDATA)
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerM2MSkjemaData>(ProsessDataKey.SØKNADSDATA)
        søknadsdata shouldBe forventetSøknadsdata

        // Verify behandling was created and set on prosessinstans (step 2)
        val behandling = prosessinstans.behandling.shouldNotBeNull()
        val fagsak = behandling.fagsak.shouldNotBeNull()
        fagsak.type shouldBe Sakstyper.EU_EOS
        fagsak.tema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
        behandling.tema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type shouldBe Behandlingstyper.FØRSTEGANG
    }
}
