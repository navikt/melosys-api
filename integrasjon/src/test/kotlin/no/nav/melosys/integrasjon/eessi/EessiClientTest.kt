package no.nav.melosys.integrasjon.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
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
import no.nav.melosys.domain.eessi.sed.OpprettBucOgSedDtoV2
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagA003Dto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.domain.eessi.sed.VedleggReferanse
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.eessi.dto.OpprettBucOgSedDto
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,

        GenericAuthFilterFactory::class,
        EessiClientConfig::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EessiClientTest(
    @Autowired private val eessiClient: EessiClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
        oAuthMockServer.reset()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun before() {
        serviceUnderTestMockServer.resetAll()
    }

    @AfterEach
    fun after() {
        MetricsTestConfig.clearMeterRegistry()
    }


    @Test
    fun opprettBucOgSed() {
        val sedDataDto = SedDataDto()
        val vedlegg = setOf(Vedlegg("pdf".toByteArray(), "tittel"))

        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}")
                )
        )

        val opprettSedDto = eessiClient.opprettBucOgSed(
            sedDataDto,
            vedlegg,
            BucType.LA_BUC_01,
            true,
            true
        )

        MetricsTestConfig.checkMetricsUri("/api/buc/{bucType}?sendAutomatisk={sendAutomatisk}&oppdaterEksisterende={oppdaterEksisterendeOmFinnes}")
        opprettSedDto.rinaSaksnummer.shouldBe("12345")

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlPathEqualTo("/api/buc/LA_BUC_01"))
                .withQueryParam("sendAutomatisk", WireMock.equalTo("true"))
                .withQueryParam("oppdaterEksisterende", WireMock.equalTo("true"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """{"sedDataDto":{"sedType":null,"utenlandskIdent":[],"bostedsadresse":null,"arbeidsgivendeVirksomheter":[],"selvstendigeVirksomheter":[],"arbeidssteder":[],"arbeidsland":[],"harFastArbeidssted":null,"lovvalgsperioder":[],"ytterligereInformasjon":null,"bruker":null,"kontaktadresse":null,"oppholdsadresse":null,"familieMedlem":[],"søknadsperiode":null,"avklartBostedsland":null,"gsakSaksnummer":null,"tidligereLovvalgsperioder":[],"mottakerIder":null,"svarAnmodningUnntak":null,"utpekingAvvis":null,"vedtakDto":null,"invalideringSedDto":null,"a008Formaal":null,"erFjernarbeidTWFA":null},"vedlegg":[{"innhold":"cGRm","tittel":"tittel","hentInnhold":"cGRm"}]}""",
                        true, false
                    )
                )
        )
    }

    @Test
    fun opprettBucOgSed_forventException() {
        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )

        shouldThrow<TekniskException> {
            eessiClient.opprettBucOgSed(
                SedDataDto(), listOf(), BucType.LA_BUC_01, true, true
            )
        }.message.shouldContain("Kall mot eessi feilet")
    }

    @Test
    fun opprettBucOgSedV2() {
        val opprettBucOgSedDtoV2 = OpprettBucOgSedDtoV2(
            bucType = BucType.LA_BUC_01,
            sedDataDto = SedDataDto(),
            vedlegg = setOf(
                VedleggReferanse("journalpostId1", "dokumentId1", "Tittel 1"),
                VedleggReferanse("journalpostId2", "dokumentId2", "Tittel 2")
            ),
            sendAutomatisk = true,
            oppdaterEksisterende = false
        )

        serviceUnderTestMockServer.stubFor(
            WireMock.any(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"rinaSaksnummer\":\"67890\",\"rinaUrl\":\"localhost:3000/rina/67890\"}")
                )
        )


        val opprettSedDto = eessiClient.opprettBucOgSedV2(opprettBucOgSedDtoV2)


        serviceUnderTestMockServer.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v2/buc"))
                .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(WireMock.equalToJson("""{"bucType":"LA_BUC_01","sedDataDto":{"sedType":null,"utenlandskIdent":[],"bostedsadresse":null,"arbeidsgivendeVirksomheter":[],"selvstendigeVirksomheter":[],"arbeidssteder":[],"arbeidsland":[],"harFastArbeidssted":null,"lovvalgsperioder":[],"ytterligereInformasjon":null,"bruker":null,"kontaktadresse":null,"oppholdsadresse":null,"familieMedlem":[],"søknadsperiode":null,"avklartBostedsland":null,"gsakSaksnummer":null,"tidligereLovvalgsperioder":[],"mottakerIder":null,"svarAnmodningUnntak":null,"utpekingAvvis":null,"vedtakDto":null,"invalideringSedDto":null,"a008Formaal":null,"erFjernarbeidTWFA":null},"vedlegg":[{"journalpostId":"journalpostId1","dokumentId":"dokumentId1","tittel":"Tittel 1"},{"journalpostId":"journalpostId2","dokumentId":"dokumentId2","tittel":"Tittel 2"}],"sendAutomatisk":true,"oppdaterEksisterende":false}""", true, false))
        )

        opprettSedDto.rinaSaksnummer.shouldBe("67890")
        opprettSedDto.rinaUrl.shouldBe("localhost:3000/rina/67890")
        MetricsTestConfig.checkMetricsUri("/api/v2/buc")
    }

    @Test
    fun opprettBucOgSedV2_forventException() {
        val opprettBucOgSedDtoV2 = OpprettBucOgSedDtoV2(
            bucType = BucType.LA_BUC_01,
            sedDataDto = SedDataDto(),
            vedlegg = emptySet(),
            sendAutomatisk = false,
            oppdaterEksisterende = false
        )

        serviceUnderTestMockServer.stubFor(
            WireMock.any(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )


        shouldThrow<TekniskException> {
            eessiClient.opprettBucOgSedV2(opprettBucOgSedDtoV2)
        }.message.shouldContain("Kall mot eessi feilet")
    }

    @Test
    fun opprettBucOgSedV2_medTommeVedlegg() {
        val opprettBucOgSedDtoV2 = OpprettBucOgSedDtoV2(
            bucType = BucType.LA_BUC_03,
            sedDataDto = SedDataDto(),
            vedlegg = emptySet(),
            sendAutomatisk = false,
            oppdaterEksisterende = true
        )

        serviceUnderTestMockServer.stubFor(
            WireMock.any(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"rinaSaksnummer\":\"99999\",\"rinaUrl\":\"localhost:3000/rina/99999\"}")
                )
        )


        val opprettSedDto = eessiClient.opprettBucOgSedV2(opprettBucOgSedDtoV2)


        serviceUnderTestMockServer.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v2/buc"))
                .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(WireMock.equalToJson("""{"bucType":"LA_BUC_03","sedDataDto":{"sedType":null,"utenlandskIdent":[],"bostedsadresse":null,"arbeidsgivendeVirksomheter":[],"selvstendigeVirksomheter":[],"arbeidssteder":[],"arbeidsland":[],"harFastArbeidssted":null,"lovvalgsperioder":[],"ytterligereInformasjon":null,"bruker":null,"kontaktadresse":null,"oppholdsadresse":null,"familieMedlem":[],"søknadsperiode":null,"avklartBostedsland":null,"gsakSaksnummer":null,"tidligereLovvalgsperioder":[],"mottakerIder":null,"svarAnmodningUnntak":null,"utpekingAvvis":null,"vedtakDto":null,"invalideringSedDto":null,"a008Formaal":null,"erFjernarbeidTWFA":null},"vedlegg":[],"sendAutomatisk":false,"oppdaterEksisterende":true}""", true, false))
        )

        opprettSedDto.rinaSaksnummer.shouldBe("99999")
    }

    @Test
    fun sendSedPåEksisterendeBuc() {
        val sedDataDto = SedDataDto()

        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(204)
                )
        )

        eessiClient.sendSedPåEksisterendeBuc(
            sedDataDto,
            "12345",
            SedType.A001
        )

        MetricsTestConfig.checkMetricsUri("/api/buc/{bucID}/sed/{sedType}")

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/api/buc/12345/sed/A001"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson("""{"sedType":null,"utenlandskIdent":[],"bostedsadresse":null,"arbeidsgivendeVirksomheter":[],"selvstendigeVirksomheter":[],"arbeidssteder":[],"arbeidsland":[],"harFastArbeidssted":null,"lovvalgsperioder":[],"ytterligereInformasjon":null,"bruker":null,"kontaktadresse":null,"oppholdsadresse":null,"familieMedlem":[],"søknadsperiode":null,"avklartBostedsland":null,"gsakSaksnummer":null,"tidligereLovvalgsperioder":[],"mottakerIder":null,"svarAnmodningUnntak":null,"utpekingAvvis":null,"vedtakDto":null,"invalideringSedDto":null,"a008Formaal":null,"erFjernarbeidTWFA":null}""", true, false)
                )
        )
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


        val institusjoner = eessiClient.hentMottakerinstitusjoner("LA_BUC_01", listOf("DE", "PL"))


        institusjoner
            .shouldHaveSize(1)
            .first().apply {
                id.shouldBe("NO:NAVT002")
                navn.shouldBe("NAVT002")
                landkode.shouldBe("NO")
            }
        MetricsTestConfig.checkMetricsUri("/api/buc/{bucType}/institusjoner?land={landkoder}")
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
            eessiClient.hentMottakerinstitusjoner("LA_BUC_01", listOf("DE"))
        }
    }


    @Test
    fun hentMelosysEessiMeldingFraJournalpostID() {
        val journalpostID = "115314"
        // MelosysEessiMelding er en integrasjons-DTO uten forTest DSL - bruker .apply
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


        val response = eessiClient.hentMelosysEessiMeldingFraJournalpostID(journalpostID)


        response.apply {
            sedType.shouldBe(melosysEessiMelding.sedType)
            journalpostId.shouldBe(melosysEessiMelding.journalpostId)
        }

        MetricsTestConfig.checkMetricsUri("/api/journalpost/{journalpostID}/eessimelding")
    }

    @Test
    fun lagreSaksrelasjon() {
        val saksrelasjonDto = SaksrelasjonDto(123L, "123", "123")

        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(204)
                )
        )

        eessiClient.lagreSaksrelasjon(saksrelasjonDto)

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/api/sak"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson("""{"gsakSaksnummer":123,"rinaSaksnummer":"123","bucType":"123"}""", true, false)
                )
        )
    }

    @Test
    fun hentSakForRinasaksnummer() {
        // SaksrelasjonDto er en integrasjons-DTO uten forTest DSL - bruker .apply
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
            eessiClient.hentSakForRinasaksnummer(saksrelasjon.rinaSaksnummer)


        saksRelasjoner
            .shouldHaveSize(1)
            .first()
            .shouldBeEqualToComparingFields(saksrelasjon, FieldsEqualityCheckConfig(ignorePrivateFields = false))

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

    @Test
    fun genererSedPdf() {
        val pdf = "pdf".toByteArray()
        val sedDataDto = SedDataDto()

        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(pdf)
                )
        )

        val resultPDF: ByteArray = eessiClient.genererSedPdf(sedDataDto, SedType.A001)

        resultPDF.shouldBe(pdf)
        MetricsTestConfig.metricsUriShouldContainBrackets()

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlPathEqualTo("/api/sed/A001/pdf"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson("""{"sedType":null,"utenlandskIdent":[],"bostedsadresse":null,"arbeidsgivendeVirksomheter":[],"selvstendigeVirksomheter":[],"arbeidssteder":[],"arbeidsland":[],"harFastArbeidssted":null,"lovvalgsperioder":[],"ytterligereInformasjon":null,"bruker":null,"kontaktadresse":null,"oppholdsadresse":null,"familieMedlem":[],"søknadsperiode":null,"avklartBostedsland":null,"gsakSaksnummer":null,"tidligereLovvalgsperioder":[],"mottakerIder":null,"svarAnmodningUnntak":null,"utpekingAvvis":null,"vedtakDto":null,"invalideringSedDto":null,"a008Formaal":null,"erFjernarbeidTWFA":null}""", true, false)
                )
        )
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


        val hentTilknyttedeBucer = eessiClient.hentTilknyttedeBucer(1L, listOf("UTKAST"))


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

        eessiClient.hentTilknyttedeBucer(1L, listOf("UTKAST", "MOTTATT", "SENDT"))

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
        eessiClient.hentTilknyttedeBucer(1L, listOf("SENDT", "UTKAST"))

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
            eessiClient.hentSedGrunnlag("1234", "abcdef")


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

        eessiClient.lukkBuc(rinaSaksnummer)

        MetricsTestConfig.metricsUriShouldContainBrackets()
    }

}

