package no.nav.melosys.integrasjon.medl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.junit5.MockKExtension
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedlemskapRestConsumerTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var restConsumer: MedlemskapRestConsumer

    @BeforeAll
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build()

        restConsumer = MedlemskapRestConsumer(webClient)
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `skal hente medlemskapsperiodeliste`() {
        val fom = LocalDate.now().minusDays(2)
        val tom = LocalDate.now().plusDays(2)
        val fnr = "12345678990"

        wireMockServer.stubFor(post(urlPathEqualTo("/rest/v1/periode/soek"))
            .withRequestBody(containing(fnr))  // Verify person identifier is in the body
            .withRequestBody(containing("personident"))  // Verify correct field name
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")
            )
        )


        restConsumer.hentPeriodeListe(fnr, fom, tom).shouldBeEmpty()


        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/rest/v1/periode/soek")))
    }

    @Test
    fun `skal hente en medlemskapsperiode`() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/medlemskapsunntak/123"))
            .withQueryParam("inkluderSporingsinfo", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")
            )
        )


        restConsumer.hentPeriode("123") shouldNotBe null
    }

    @Test
    fun `skal kaste RuntimeException ved oppdatering`() {
        wireMockServer.stubFor(put(urlPathEqualTo("/api/v1/medlemskapsunntak"))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("Validering feilet")
            )
        )


        val exception = shouldThrow<RuntimeException> { 
            restConsumer.oppdaterPeriode(MedlemskapsunntakForPut()) 
        }


        exception.message shouldContain "400 Bad Request from PUT"
    }

    @Test
    fun `skal oppdatere periode`() {
        val medlemskapsunntakForPut = MedlemskapsunntakForPut().apply {
            unntakId = 12345L
        }

        wireMockServer.stubFor(put(urlPathEqualTo("/api/v1/medlemskapsunntak"))
            .withRequestBody(matchingJsonPath("$.unntakId", equalTo("12345")))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")
            )
        )


        restConsumer.oppdaterPeriode(medlemskapsunntakForPut) shouldNotBe null
    }
}