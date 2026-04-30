package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
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

class DigitalSøknadMottakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"

    @Test
    fun `mottak av digital søknad starter saga og henter søknadsdata fra melosys-skjema-api`() {
        // Bruker fnr fra PersonRepo i melosys-mock (KARAFFEL TRIVIELL)
        val testFnr = "30056928150"

        val forventetSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            orgnr = "AGORG12345"
        }
        val skjemaId = forventetSøknadsdata.skjema.id

        // Stub melosys-skjema-api endpoint for søknadsdata
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(forventetSøknadsdata))
                )
        )

        // Stub melosys-skjema-api endpoint for PDF
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/pdf"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody("PDF content".toByteArray())
                )
        )

        // Stub melosys-skjema-api endpoint for saksnummer-registrering
        mockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/saksnummer"))
                .willReturn(WireMock.aResponse().withStatus(204))
        )

        val melding = SkjemaMottattMelding(skjemaId)

        // Send Kafka message
        kafkaTemplate.send(kafkaTopic, melding)

        // Wait for saga to complete
        await.atMost(Duration.ofSeconds(10)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        // Fetch and verify prosessinstans state
        val prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(skjemaId.toString()).single()

        // Basic prosessinstans info
        prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
        prosessinstans.status shouldBe ProsessStatus.FERDIG
        prosessinstans.låsReferanse shouldBe skjemaId.toString()

        // All steps completed successfully (SEND_SAKSNUMMER_TIL_SKJEMA is the last step)
        prosessinstans.sistFullførtSteg shouldBe ProsessSteg.SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API
        prosessinstans.hendelser.shouldHaveSize(0)

        // Verify data stored by consumer (DIGITAL_SØKNAD_SKJEMA_ID)
        val lagretSkjemaId = prosessinstans.hentData<UUID>(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID)
        lagretSkjemaId shouldBe skjemaId

        // Verify data stored by step 1 (SØKNADSDATA)
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        søknadsdata.referanseId shouldBe forventetSøknadsdata.referanseId
        søknadsdata.skjema.fnr shouldBe forventetSøknadsdata.skjema.fnr

        // Verify behandling was created and set on prosessinstans (step 2)
        val behandling = prosessinstans.behandling.shouldNotBeNull()
        val fagsak = behandling.fagsak.shouldNotBeNull()
        fagsak.type shouldBe Sakstyper.EU_EOS
        fagsak.tema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
        behandling.tema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type shouldBe Behandlingstyper.FØRSTEGANG

        // Verify journalpostId was set on behandling (step 3)
        behandling.initierendeJournalpostId.shouldNotBeNull()

        // Verify mottatteOpplysninger was created on behandling (step 4)
        val mottatteOpplysninger = behandling.mottatteOpplysninger.shouldNotBeNull()

        // Verify sidemeny-mapping: hovedarbeidsgivers orgnr pre-fylt i juridiskArbeidsgiverNorge
        val soeknad = mottatteOpplysninger.mottatteOpplysningerData as Soeknad
        soeknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("AGORG12345")
    }
}
