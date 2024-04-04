package no.nav.melosys.integrasjon.eessi

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

@Import(
    StsWebClientProducer::class,
    SecurityTokenServiceConsumer::class,
    RestSTSService::class,
    OAuthMockServer::class,

    GenericAuthFilterFactory::class,
    EessiConsumerProducerConfig::class,
)
@WebMvcTest
@AutoConfigureWebClient
@EnableOAuth2Client
@ActiveProfiles("wiremock-test")
class EessiConsumerTokenTest(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, List<String>>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer -- user_access_token --")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeFromController()
    }


    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeRequest()
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

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).contains(errorFromServerMessage())
        }
    }

    @Test
    fun `Skal feile om token ikke stemmer overens`() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --feil token--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        shouldThrow<TekniskException> {
            executeFromSystem()
        }.message.shouldContain("Authorization: Bearer --feil token--")
    }

    override fun getMockData(): String = "[]"

    override fun executeRequest() =
        eessiConsumer.hentMuligeAksjoner("123")
}
