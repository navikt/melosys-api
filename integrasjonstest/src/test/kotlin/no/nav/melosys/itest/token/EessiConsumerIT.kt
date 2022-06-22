package no.nav.melosys.itest.token

import no.nav.melosys.integrasjon.eessi.EessiConsumer
import no.nav.melosys.integrasjon.eessi.EessiConsumerImpl
import no.nav.melosys.integrasjon.eessi.EessiConsumerProducer
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        MockRestServerProvider::class,

        EessiConsumerImpl::class,
        EessiConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class EessiConsumerIT(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Autowired private val mockRestServerProvider: MockRestServerProvider
) : StsTestBase<String>(mockRestServerProvider) {

    private fun verifyEessiHeaders(headers: Map<String, String>) {
        mockRestServerProvider.getServiceUnderTestMockServer()
            .expect(requestTo("/buc/123/aksjoner"))
            .andExpect(method(HttpMethod.GET)).apply {
                headers.forEach {
                    andExpect(header(it.key, it.value))
                }
            }
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    "[]", MediaType.APPLICATION_JSON
                )
            )
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyEessiHeaders(
                mapOf(
                    Pair("Authorization", "Bearer --token-from-system--"),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyEessiHeaders(
                mapOf(
                    Pair("Authorization", "Bearer --token-from-user--"),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyEessiHeaders(
            mapOf(
                Pair("Authorization", "Bearer --token-from-system--"),
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).contains(errorFromServerMessage())
        }
    }

    override fun stubError() {
        mockRestServerProvider.getServiceUnderTestMockServer()
            .expect(requestTo("/buc/123/aksjoner"))
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("error")
            )

    }

    override fun errorFromServerMessage() = "500 Internal Server Error: \"error\""

    override fun executeRequest() {
        eessiConsumer.hentMuligeAksjoner("123")
    }
}

