package no.nav.melosys.integrasjon.inngangsvilkar

import java.time.LocalDate
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.TestInstance

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
        val statsborgerskap = setOf(Land.av(Land.NORGE))
        val arbeidsland = setOf(Land.SVERIGE)
        val flereLandUkjentHvilke = false
        val nå = LocalDate.now()

        val expectedRequest = VurderInngangsvilkaarRequest(
            statsborgerskap = setOf(Land.NORGE),
            arbeidsland = arbeidsland,
            flereLandUkjentHvilke = flereLandUkjentHvilke,
            periode = Periode(nå, nå)
        )

        val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
        val expectedJsonRequestBody = objectMapper.writeValueAsString(expectedRequest)

        stubFor(any(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""{"kvalifisererForEf883_2004": true,"feilmeldinger": []}""")))

        val response = inngangsvilkaarConsumer.vurderInngangsvilkår(statsborgerskap, arbeidsland, flereLandUkjentHvilke, Periode(nå, nå))


        verify(postRequestedFor(urlPathEqualTo("/inngangsvilkaar"))
            .withRequestBody(equalToJson(expectedJsonRequestBody)))

        response.run {
            kvalifisererForEf883_2004 shouldBe true
        }
    }
}
