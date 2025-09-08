package no.nav.melosys.integrasjon.trygdeavgift

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.readResourceAsString
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrygdeavgiftConsumerTest {

    private lateinit var trygdeavgiftConsumer: TrygdeavgiftConsumer

    private lateinit var mockServer: MockWebServer

    private val url by lazy { "http://localhost:${mockServer.port}" }

    @BeforeAll
    fun setupServer() {
        mockServer = MockWebServer().apply { start() }
    }

    @AfterAll
    fun tearDown() {
        mockServer.shutdown()
    }

    @BeforeEach
    fun setup() {
        trygdeavgiftConsumer = TrygdeavgiftConsumer(url)
    }

    @Test
    fun `beregn trygdeavgift`() {
        mockServer.enqueue(
            MockResponse()
                .setBody(readResourceAsString("mock/trygdeavgift/trygdeavgift.json"))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )


        val response = trygdeavgiftConsumer.beregnTrygdeavgift(lagTrygdeavgiftsberegningRequest())


        response[0].run {
            beregnetPeriode.sats shouldBe BigDecimal.valueOf(21.8)
            beregnetPeriode.månedsavgift shouldBe PengerDto(BigDecimal.valueOf(21800))
            component2() shouldNotBe null
        }
    }

    private fun lagTrygdeavgiftsberegningRequest() = TrygdeavgiftsberegningRequest(
        emptySet(),
        emptySet(),
        emptyList(),
        LocalDate.now().minusYears(20)
    )
}
