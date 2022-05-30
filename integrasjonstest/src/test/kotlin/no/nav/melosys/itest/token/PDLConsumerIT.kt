package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.pdl.*
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClientClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClientClient::class,
        WebClientAutoConfiguration::class,

        PDLConsumerImpl::class,
        PDLConsumerProducer::class,
        PDLAuthFilter::class,
        PDLAuthFilterProducer::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class PDLConsumerIT(
    @Autowired private val pdlConsumer: PDLConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerTestBase<String>(server, mockPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot PDL feilet.")
        }
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.post("/graphql")
    }

    override fun getMockData(): String {
        return """{
          "data": {
            "hentIdenter": {
              "identer": [
                {
                  "ident": "99026522600",
                  "gruppe": "FOLKEREGISTERIDENT"
                },
                {
                  "ident": "9834873315250",
                  "gruppe": "AKTORID"
                }
              ]
            }
          }
        }
        """
    }

    override fun executeRequest() {
        pdlConsumer.hentIdenter("0")
    }
}

