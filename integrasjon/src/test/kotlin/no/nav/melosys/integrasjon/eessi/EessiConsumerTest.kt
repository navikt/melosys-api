package no.nav.melosys.integrasjon.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagA003Dto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.StsMockServer
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.*

@Import(
    OAuthMockServer::class,
    CorrelationIdOutgoingFilter::class,
    StsWebClientProducer::class,
    StsMockServer::class,
    RestSTSService::class,

    GenericAuthFilterFactory::class,
    EessiConsumerProducerConfig::class,
    MetricsTestConfig::class,
    FakeUnleash::class
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EessiConsumerTest(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val stsMockServer: StsMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {
    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
        oAuthMockServer.reset()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
    }

    @BeforeEach
    fun before() {
        serviceUnderTestMockServer.resetAll()
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
        MetricsTestConfig.clearMeterRegistry()
    }


    @Test
    fun opprettBucOgSed() {
        val sedDataDto = SedDataDto()
        val vedlegg = setOf(Vedlegg("pdf".toByteArray(), "tittel"))
        val json = ObjectMapper().writeValueAsString(
            mapOf(
                "sedDataDto" to sedDataDto,
                "vedlegg" to vedlegg
            )
        )
        serviceUnderTestMockServer.stubFor(
            post("/api/buc/LA_BUC_01?sendAutomatisk=true&oppdaterEksisterende=true")
                .withRequestBody(WireMock.equalToJson(json))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}")
                )
        )


        val opprettSedDto = eessiConsumer.opprettBucOgSed(
            sedDataDto,
            vedlegg,
            BucType.LA_BUC_01,
            true,
            true
        )

        MetricsTestConfig.checkMetricsUri("/buc/{bucType}?sendAutomatisk={sendAutomatisk}&oppdaterEksisterende={oppdaterEksisterendeOmFinnes}")

        opprettSedDto.rinaSaksnummer.shouldBe("12345")
    }

    @Test
    fun opprettBucOgSed_forventException() {
        serviceUnderTestMockServer.stubFor(
            post("/api/buc/LA_BUC_01?sendAutomatisk=true&oppdaterEksisterende=true")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )


        shouldThrow<TekniskException> {
            eessiConsumer.opprettBucOgSed(
                SedDataDto(), listOf(), BucType.LA_BUC_01, true, true
            )
        }.message.shouldContain("Kall mot eessi feilet")
    }

    @Test
    fun sendSedPåEksisterendeBuc() {
        val sedDataDto = SedDataDto()
        serviceUnderTestMockServer.stubFor(
            post("/api/buc/12345/sed/A001")
                .withRequestBody(WireMock.equalToJson((ObjectMapper().writeValueAsString(sedDataDto))))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(204)
                )
        )


        eessiConsumer.sendSedPåEksisterendeBuc(
            sedDataDto,
            "12345",
            SedType.A001
        )

        MetricsTestConfig.checkMetricsUri("/buc/{bucID}/sed/{sedType}")
    }

    @Test
    fun hentMottakerinstitusjoner() {
        serviceUnderTestMockServer.stubFor(
            get("/api/buc/LA_BUC_01/institusjoner?land=DE%2CPL")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]")
                )
        )


        val institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", listOf("DE", "PL"))


        institusjoner
            .shouldHaveSize(1)
            .first().apply {
                id.shouldBe("NO:NAVT002")
                navn.shouldBe("NAVT002")
                landkode.shouldBe("NO")
            }
        MetricsTestConfig.checkMetricsUri("/buc/{bucType}/institusjoner?land={landkoder}")
    }

    @Test
    fun hentMottakerinstitusjoner_bodyFraServerMangler() {
        serviceUnderTestMockServer.stubFor(
            get("/api/buc/LA_BUC_01/institusjoner?land=DE")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                )
        )


        shouldThrow<NullPointerException> {
            eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", listOf("DE"))
        }
    }


    @Test
    fun hentMelosysEessiMeldingFraJournalpostID() {
        val journalpostID = "115314"
        val melosysEessiMelding = MelosysEessiMelding().apply {
            sedType = "A009"
            journalpostId = journalpostID
        }
        serviceUnderTestMockServer.stubFor(
            get("/api/journalpost/$journalpostID/eessimelding")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(ObjectMapper().writeValueAsString(melosysEessiMelding))
                )
        )


        val response = eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(journalpostID)


        response.apply {
            sedType.shouldBe(melosysEessiMelding.sedType)
            journalpostId.shouldBe(melosysEessiMelding.journalpostId)
        }

        MetricsTestConfig.checkMetricsUri("/journalpost/{journalpostID}/eessimelding")
    }

    @Test
    fun lagreSaksrelasjon() {
        val saksrelasjonDto = SaksrelasjonDto(123L, "123", "123")
        serviceUnderTestMockServer.stubFor(
            post("/api/sak")
                .withRequestBody(WireMock.equalToJson(ObjectMapper().writeValueAsString(saksrelasjonDto)))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(204)
                )
        )

        eessiConsumer.lagreSaksrelasjon(saksrelasjonDto)
    }

    @Test
    fun hentSakForRinasaksnummer() {
        val saksrelasjon = SaksrelasjonDto().apply {
            rinaSaksnummer = "114422"
            gsakSaksnummer = 123L
            bucType = "LA_BUC_04"
        }
        serviceUnderTestMockServer.stubFor(
            get("/api/sak?rinaSaksnummer=${saksrelasjon.rinaSaksnummer}")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(ObjectMapper().writeValueAsString(listOf(saksrelasjon)))
                )
        )


        val saksRelasjoner: List<SaksrelasjonDto> =
            eessiConsumer.hentSakForRinasaksnummer(saksrelasjon.rinaSaksnummer)


        saksRelasjoner
            .shouldHaveSize(1)
            .first()
            .shouldBeEqualToComparingFields(saksrelasjon, FieldsEqualityCheckConfig(ignorePrivateFields = false))

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun genererSedPdf() {
        val pdf = "pdf".toByteArray()
        serviceUnderTestMockServer.stubFor(
            post("/api/sed/A001/pdf")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(pdf)
                )
        )


        val resultPDF: ByteArray = eessiConsumer.genererSedPdf(SedDataDto(), SedType.A001)


        resultPDF.shouldBe(pdf)

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun hentTilknyttedeBucer_medEnStatus_forventBucer() {
        serviceUnderTestMockServer.stubFor(
            get("/api/sak/1/bucer?statuser=UTKAST")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(javaClass.classLoader.getResource("mock/eux/bucer.json")!!.readText())
                )
        )


        val hentTilknyttedeBucer = eessiConsumer.hentTilknyttedeBucer(1L, listOf("UTKAST"))


        hentTilknyttedeBucer
            .shouldHaveSize(2)
            .apply {
                first().apply {
                    id.shouldBe("111111")
                    bucType.shouldBe("LA_BUC_03")
                    erÅpen().shouldBe(true)
                    opprettetDato.shouldBe(LocalDate.of(2019, 4, 4))
                    seder.shouldBeSingleton {
                        it.sedId.shouldBe("22223333")
                        it.sedType.shouldBe("A008")
                        it.status.shouldBe("UTKAST")
                    }
                }
                last().apply {
                    id.shouldBe("222222")
                    bucType.shouldBe("LA_BUC_01")
                    erÅpen().shouldBe(false)
                    opprettetDato.shouldBe(LocalDate.of(2019, 4, 4))
                    seder.shouldHaveSize(2).apply {
                        first().apply {
                            sedId.shouldBe("11221122")
                            sedType.shouldBe("A002")
                            status.shouldBe("UTKAST")
                        }
                        last().apply {
                            sedId shouldBe ("11332233")
                            sedType.shouldBe("A001")
                            status.shouldBe("UTKAST")
                        }
                    }
                }
            }

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun hentTilknyttedeBucer_medFlereStatuser_forventRettSti() {
        serviceUnderTestMockServer.stubFor(
            get("/api/sak/1/bucer?statuser=UTKAST%2CMOTTATT%2CSENDT")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")
                )
        )

        eessiConsumer.hentTilknyttedeBucer(1L, listOf("UTKAST", "MOTTATT", "SENDT"))

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun hentTilknyttedeBucer_medToStatuser_forventRettSti() {
        serviceUnderTestMockServer.stubFor(
            get("/api/sak/1/bucer?statuser=SENDT%2CUTKAST")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")
                )
        )
        eessiConsumer.hentTilknyttedeBucer(1L, listOf("SENDT", "UTKAST"))

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun hentSedGrunnlag() {
        serviceUnderTestMockServer.stubFor(
            get("/api/buc/1234/sed/abcdef/grunnlag")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"sedType\": \"A003\"}")
                )
        )

        val response: SedGrunnlagDto =
            eessiConsumer.hentSedGrunnlag("1234", "abcdef")


        response.shouldBeInstanceOf<SedGrunnlagA003Dto>()

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun lukkBuc() {
        val rinaSaksnummer = "1424"
        serviceUnderTestMockServer.stubFor(
            WireMock.post("/api/buc/$rinaSaksnummer/lukk")
                .withRequestBody(WireMock.absent())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(204)
                )
        )

        eessiConsumer.lukkBuc(rinaSaksnummer)

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    fun get(url: String): MappingBuilder =
        WireMock.get(url)
            .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
            .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))

    fun post(url: String): MappingBuilder =
        WireMock.post(url)
            .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
            .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))

}
