package no.nav.melosys.integrasjon.pdl

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(
    StsWebClientProducer::class,
    RestTokenServiceClient::class,
    OAuthMockServer::class,

    PDLConsumerProducer::class,
    PDLAuthFilter::class
)
@WebMvcTest
@AutoConfigureWebClient
@EnableOAuth2Client
@ActiveProfiles("wiremock-test")
class PDLConsumerTokenTest(
    @Autowired private val pdlConsumer: PDLConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, Identliste>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        setupWireMock {
            it.withHeader("Authorization", WireMock.equalTo("Bearer --token-from-system--"))
            it.withHeader("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
        }
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        setupWireMock {
            it.withHeader("Authorization", WireMock.equalTo("Bearer -- user_access_token --"))
            it.withHeader("Nav-Consumer-Token", WireMock.absent())
        }
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        setupWireMock {
            it.withHeader("Authorization", WireMock.equalTo("Bearer --token-from-system--"))
            it.withHeader("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
        }
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot PDL feilet.")
        }
    }

    @Test
    fun correlationIdLeggesPåRequest() {
        verifyHeaders(
            mapOf(
                Pair("X-Correlation-ID", WireMock.matching(UUID_REGEX)),
            )
        )
        executeRequest()
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

    override fun executeRequest() =
        pdlConsumer.hentIdenter("0")
}
