package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
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
 * Integrasjonstest for full serialisering per søknadsgruppe (MELOSYS-8151).
 *
 * Tre relaterte deler (arbeidstaker v1, arbeidsgiver AG, ny versjon v2) sendes med SAMME gruppeId
 * tilnærmet samtidig. Lås-prefikset = gruppeId, så PÅ_VENT-mekanismen serialiserer flyten: én del
 * behandles om gangen. Resultat: nøyaktig én fagsak, alle prosessinstanser FERDIG, og ingen feiler
 * på UQ_VILKAARSRESULTAT (som ville skjedd om VURDER_INNGANGSVILKÅR kjørte samtidig på delt behandling).
 */
class DigitalSøknadSerialiseringIT(
    @Autowired @Qualifier("skjemaMottattMelding")
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.skjema.innsendt.v1-local"
    private val testFnr = "30056928150"

    @Test
    fun `relaterte deler med samme gruppeId serialiseres til én sak uten vilkårsresultat-feil`() {
        val v1 = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
        val v1Id = v1.skjema.id
        val ag = lagUtsendtArbeidstakerSkjemaM2MDto {
            fnr = testFnr
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }.copy(kobletSkjema = lagKobletArbeidsgiverSkjema(v1Id))
        val agId = ag.skjema.id
        val v2 = lagUtsendtArbeidstakerSkjemaM2MDto { fnr = testFnr }
            .copy(kobletSkjema = lagKobletArbeidsgiverSkjema(v1Id))
        val v2Id = v2.skjema.id

        stubSkjemaEndpoints(v1Id, v1)
        stubSkjemaEndpoints(agId, ag)
        stubSkjemaEndpoints(v2Id, v2)

        // Samme gruppeId for alle tre → serialiseres. Sendes uten pause (kappløp).
        val gruppeId = v1Id
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(v1Id, emptyList(), gruppeId))
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(agId, listOf(v1Id), gruppeId))
        kafkaTemplate.send(kafkaTopic, SkjemaMottattMelding(v2Id, listOf(v1Id, agId), gruppeId))

        val alleIder = listOf(v1Id, agId, v2Id)
        await.atMost(Duration.ofSeconds(40)).until {
            alleIder.all { id ->
                prosessinstansRepository.findAllByLåsReferanseStartingWith("${gruppeId}_$id")
                    .firstOrNull()?.status == ProsessStatus.FERDIG
            }
        }

        // Ingen prosessinstans feilet (serialisering hindret vilkårsresultat-racet)
        val prosessinstanser = alleIder.map {
            prosessinstansRepository.findAllByLåsReferanseStartingWith("${gruppeId}_$it").single()
        }
        prosessinstanser.forEach { it.status shouldBe ProsessStatus.FERDIG }
        prosessinstanser.flatMap { it.hendelser } shouldHaveSize 0

        // Nøyaktig én sak for alle tre delene
        skjemaSakMappingRepository.findBySkjemaIdIn(alleIder)
            .map { it.saksnummer }.distinct() shouldHaveSize 1
    }

    private fun lagKobletArbeidsgiverSkjema(id: UUID): UtsendtArbeidstakerSkjemaDto =
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
                    WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(søknadsdata))
                )
        )
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/pdf"))
                .willReturn(
                    WireMock.aResponse().withStatus(200)
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
