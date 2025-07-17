package no.nav.melosys.integrasjon.tilgangsmaskinen

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.TilgangsmaskinenProblemDetail
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import no.nav.melosys.integrasjon.tilgangsmaskinen.TilgangsmaskinenException

class TilgangsmaskinenConsumerImplTest {

    companion object {
        private lateinit var mockServer: MockWebServer
        private val objectMapper = ObjectMapper()

        @JvmStatic
        @BeforeAll
        fun setupServer() {
            mockServer = MockWebServer()
            mockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }
    }

    private lateinit var tilgangsmaskinenConsumer: TilgangsmaskinenConsumer

    @BeforeEach
    fun setup() {
        tilgangsmaskinenConsumer = TilgangsmaskinenConsumerImpl(
            WebClient.builder()
                .baseUrl("http://localhost:${mockServer.port}")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build()
        )
    }

    @Test
    fun `sjekkTilgang med kjerne regeltype - skal returnere true ved 204 respons`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val harTilgang = tilgangsmaskinenConsumer.sjekkTilgang("12345678901", RegelType.KJERNE_REGELTYPE)

        harTilgang shouldBe true

        // Verifiser request
        val request = mockServer.takeRequest()
        request.path shouldBe "/api/v1/kjerne"
        request.method shouldBe "POST"
        request.body.readUtf8() shouldBe "12345678901"
    }

    @Test
    fun `sjekkTilgang med komplett regeltype - skal bruke riktig endepunkt`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val harTilgang = tilgangsmaskinenConsumer.sjekkTilgang("12345678901", RegelType.KOMPLETT_REGELTYPE)

        harTilgang shouldBe true

        // Verifiser request
        val request = mockServer.takeRequest()
        request.path shouldBe "/api/v1/komplett"
    }

    @Test
    fun `sjekkTilgang - skal returnere false ved 403 respons`() {
        val problemDetail = TilgangsmaskinenProblemDetail(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            title = "AVVIST_STRENGT_FORTROLIG_ADRESSE",
            status = 403,
            instance = "Z990883/03508331575",
            brukerIdent = "03508331575",
            navIdent = "Z990883",
            traceId = "444290be30ed4fdd9a849654bad9dc1b",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            kanOverstyres = false
        )

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(problemDetail))
        )

        val harTilgang = tilgangsmaskinenConsumer.sjekkTilgang("03508331575")

        harTilgang shouldBe false
    }

    @Test
    fun `sjekkTilgang - skal kaste exception ved 404 respons`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Person ikke funnet\"}")
        )

        shouldThrow<TilgangsmaskinenException> {
            tilgangsmaskinenConsumer.sjekkTilgang("12345678901")
        }
    }

    @Test
    fun `sjekkTilgang - skal kaste exception ved 500 respons`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Internal Server Error\"}")
        )

        shouldThrow<TilgangsmaskinenException> {
            tilgangsmaskinenConsumer.sjekkTilgang("12345678901")
        }
    }
}
