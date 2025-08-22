package no.nav.melosys.integrasjon.soknadmottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.msm.AltinnDokument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoknadMottakConsumerImplTest {

    private lateinit var soknadMottakConsumer: SoknadMottakConsumer
    private lateinit var wireMockServer: WireMockServer

    private val søknadID = "grj304iht"

    @BeforeAll
    fun `initial setup`() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val webClient = WebClient.builder()
            .baseUrl(wireMockServer.baseUrl())
            .build()

        soknadMottakConsumer = SoknadMottakConsumerImpl(webClient)
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentSøknad - mottar søknad i xml - søknad blir mappet til struktur`() {
        val xmlResponse = javaClass.classLoader.getResource("soknad_altinn.xml")
            ?.readText()
            ?: error("Kunne ikke lest inn soknad_altinn.xml")

        wireMockServer.stubFor(
            get(urlMatching(".*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                        .withBody(xmlResponse)
                )
        )

        val søknad = soknadMottakConsumer.hentSøknad(søknadID)

        søknad.run {
            innhold.shouldNotBeNull().run {
                midlertidigUtsendt.arbeidsland shouldBe Landkoder.BG.kode
            }
        }

        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/soknader/$søknadID"))
                .withHeader("Accept", matching(MediaType.APPLICATION_XML_VALUE))
        )
    }

    @Test
    fun `hentDokumenter - mottar liste av dokumenter - blir mappet`() {
        val altinnDokument = AltinnDokument(
            søknadID, "dokID123", "tittel", "Fullmakt", "Base64EncodedPdf", Instant.MIN
        )

        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        val jsonResponseBody = objectMapper.writeValueAsString(setOf(altinnDokument))

        wireMockServer.stubFor(
            get(urlMatching(".*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponseBody)
                )
        )

        val dokumenter = soknadMottakConsumer.hentDokumenter(søknadID)

        dokumenter.run {
            size shouldBe 1
        }

        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/soknader/$søknadID/dokumenter"))
                .withHeader("Accept", matching(MediaType.APPLICATION_JSON_VALUE))
        )
    }
}
