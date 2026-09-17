package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

/**
 * Integrasjonstest for MELOSYS-8151: en digital-søknad-melding som rutes til NY-flyten skal,
 * når den under DB-låsen oppdager at saken likevel finnes, feste seg på den eksisterende saken
 * i stedet for å opprette en duplikat fagsak.
 *
 * Scenario (deterministisk simulering av kappløpet):
 * 1. Arbeidstakers del (v1) sendes og oppretter fagsak X.
 * 2. Arbeidsgivers del (AG) sendes med tom `relaterteSkjemaIder` slik at consumeren IKKE finner
 *    saken og ruter til NY-flyten (akkurat som når den taper kappløpet mot v1). AG sin M2M-DTO er
 *    likevel koblet til v1 (kobletSkjema = v1), slik at re-sjekken under låsen finner sak X.
 * 3. AG skal feste seg på sak X — ingen ny fagsak.
 *
 * Før fiks: AG ville opprettet en andre fagsak (og feilet på OPPRETT_ARKIVSAK ved attach).
 */
class DigitalSøknadDuplikatSakIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150" // KARAFFEL TRIVIELL fra PersonRepo i melosys-mock

    @Test
    fun `NY-flyt fester på eksisterende sak under låsen i stedet for å opprette duplikat`() {
        // --- Steg 1: v1 oppretter fagsak ---
        val v1 = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val v1Id = v1.skjema.id
        stubSkjemaEndpoints(v1Id, v1)
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(v1Id))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(v1Id.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }
        val saksnummerX = prosessinstansRepository.findAllByLåsReferanseStartingWith(v1Id.toString())
            .single().behandling.shouldNotBeNull().fagsak.saksnummer

        // --- Steg 2: AG sendes som NY-flyt (tom relaterteSkjemaIder), men DTO-en er koblet til v1 ---
        val ag = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }.copy(kobletSkjema = lagKobletSkjemaMedId(v1Id))
        val agId = ag.skjema.id
        stubSkjemaEndpoints(agId, ag)

        // relaterteSkjemaIder bevisst tom → consumeren finner ingen sak → NY-flyt
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(agId))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(agId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        // --- Steg 3: AG skal være festet på sak X, ikke ha opprettet ny ---
        val agProsessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(agId.toString()).single()
        agProsessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD // rutet NY, men festet
        agProsessinstans.status shouldBe ProsessStatus.FERDIG
        agProsessinstans.hendelser shouldHaveSize 0 // ingen feil (f.eks. OPPRETT_ARKIVSAK)
        agProsessinstans.finnData<Boolean>(ProsessDataKey.DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE) shouldBe true
        agProsessinstans.behandling.shouldNotBeNull().fagsak.saksnummer shouldBe saksnummerX

        // Begge skjema peker på nøyaktig én sak
        skjemaSakMappingRepository.findBySkjemaIdIn(listOf(v1Id, agId))
            .map { it.saksnummer }.distinct() shouldHaveSize 1
    }

    @Test
    fun `rot-innsending fester på sak opprettet av et barn via claim (rekkefølge-uavhengig)`() {
        // --- Steg 1: AG (barn) prosesseres FØRST og oppretter saken, koblet til v1 ---
        // AG sendes som NY-flyt (tom relaterteSkjemaIder), men DTO-en er koblet til v1 som ennå ikke
        // er mottatt. AG oppretter saken og «claimer» v1 mot den.
        val v1Id = UUID.randomUUID()
        val ag = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }.copy(kobletSkjema = lagKobletSkjemaMedId(v1Id))
        val agId = ag.skjema.id
        stubSkjemaEndpoints(agId, ag)
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(agId))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(agId.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }
        val saksnummer = prosessinstansRepository.findAllByLåsReferanseStartingWith(agId.toString())
            .single().behandling.shouldNotBeNull().fagsak.saksnummer

        // --- Steg 2: rot-innsendingen v1 prosesseres etterpå, uten egne referanser ---
        // v1 refererer ingen andre skjema; den finner saken kun via claim-raden AG skrev.
        val v1 = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }.copy(
            skjema = lagArbeidstakerSkjemaMedId(v1Id)
        )
        stubSkjemaEndpoints(v1Id, v1)
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(v1Id))

        await.atMost(Duration.ofSeconds(15)).until {
            prosessinstansRepository.findAllByLåsReferanseStartingWith(v1Id.toString())
                .firstOrNull()?.status == ProsessStatus.FERDIG
        }

        // Claim-raden gjør at selv consumeren finner saken via v1s egen id → ruter EKSISTERENDE-flyt.
        // Rot-innsendingen fester seg dermed på samme sak uavhengig av at den ble prosessert sist.
        val v1Prosessinstans = prosessinstansRepository.findAllByLåsReferanseStartingWith(v1Id.toString()).single()
        v1Prosessinstans.type shouldBe ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
        v1Prosessinstans.status shouldBe ProsessStatus.FERDIG
        v1Prosessinstans.hendelser shouldHaveSize 0
        v1Prosessinstans.behandling.shouldNotBeNull().fagsak.saksnummer shouldBe saksnummer

        skjemaSakMappingRepository.findBySkjemaIdIn(listOf(v1Id, agId))
            .map { it.saksnummer }.distinct() shouldHaveSize 1
    }

    private fun lagArbeidstakerSkjemaMedId(id: UUID): UtsendtArbeidstakerSkjemaDto =
        UtsendtArbeidstakerSkjemaDto(
            id = id,
            status = SkjemaStatus.SENDT,
            fnr = testFnr,
            orgnr = "123456789",
            opprettetDato = LocalDateTime.now(),
            endretDato = LocalDateTime.now(),
            metadata = no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = "987654321",
                arbeidstakerNavn = "Test Arbeidstaker"
            ),
            data = no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
        )

    private fun lagKobletSkjemaMedId(id: UUID): UtsendtArbeidstakerSkjemaDto =
        UtsendtArbeidstakerSkjemaDto(
            id = id,
            status = SkjemaStatus.SENDT,
            fnr = testFnr,
            orgnr = "123456789",
            opprettetDato = LocalDateTime.now(),
            endretDato = LocalDateTime.now(),
            metadata = ArbeidsgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = "987654321",
                arbeidstakerNavn = "Test Arbeidstaker"
            ),
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        )

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
