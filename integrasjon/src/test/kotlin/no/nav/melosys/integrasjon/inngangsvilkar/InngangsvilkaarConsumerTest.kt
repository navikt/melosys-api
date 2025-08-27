package no.nav.melosys.integrasjon.inngangsvilkar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InngangsvilkaarConsumerTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var inngangsvilkaarConsumer: InngangsvilkaarConsumer

    @BeforeEach
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()
        configureFor(wireMockServer.port())

        val url = "http://localhost:${wireMockServer.port()}"
        val webClient = WebClient.builder()
            .baseUrl(url)
            .build()
        inngangsvilkaarConsumer = InngangsvilkaarConsumer(webClient)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `vurderInngangsvilkår`() {

        val vurderInngangsvilkaarRequest = VurderInngangsvilkaarRequest(
            statsborgerskap = setOf(Land.NORGE),
            arbeidsland = setOf(Land.SVERIGE),
            flereLandUkjentHvilke = false,
            periode = Periode(LocalDate.now(), LocalDate.now())
        )

        val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

        stubFor(any(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""{"kvalifisererForEf883_2004": true,"feilmeldinger": []}""")))

        val response = inngangsvilkaarConsumer.vurderInngangsvilkår(vurderInngangsvilkaarRequest)


        verify(postRequestedFor(urlPathEqualTo("/inngangsvilkaar"))
            .withRequestBody(equalToJson(
                objectMapper.writeValueAsString(vurderInngangsvilkaarRequest)
            ))
        )

        response.run {
            kvalifisererForEf883_2004 shouldBe true
        }
    }
}
