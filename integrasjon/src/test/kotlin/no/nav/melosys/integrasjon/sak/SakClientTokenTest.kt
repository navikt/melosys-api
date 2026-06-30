package no.nav.melosys.integrasjon.sak

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.ClientWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration


@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        SakClientConfig::class,
        GenericAuthFilterFactory::class,
        OAuthMockServer::class
    ]
)
@AutoConfigureWebClient
class SakClientTokenTest(
    @Autowired private val sakClient: SakClientInterface,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ClientWireMockTestBase<String, SakDto>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer -- user_access_token --")),
            )
        )
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot Sak API feilet")
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

    override fun getMockData(): String {
        return "{}"
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.post(UrlPattern.ANY)
    }

    override fun executeRequest() =
        sakClient.opprettSak(SakDto())
}
