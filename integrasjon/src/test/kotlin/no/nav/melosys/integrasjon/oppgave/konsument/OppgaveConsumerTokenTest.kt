package no.nav.melosys.integrasjon.oppgave.konsument

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto
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
import org.springframework.test.context.ActiveProfiles

@Import(
    StsWebClientProducer::class,
    SecurityTokenServiceConsumer::class,
    RestSTSService::class,
    OAuthMockServer::class,

    OppgaveConsumer::class,
    OppgaveConsumerProducer::class,
    GenericAuthFilterFactory::class
)
@WebMvcTest
@ActiveProfiles("wiremock-test")
@EnableOAuth2Client
@AutoConfigureWebClient
class OppgaveConsumerTokenTest(
    @Autowired private val oppgaveConsumer: OppgaveConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, OppgaveDto>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
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
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
            )
        )
        executeRequest()
    }


    @Test
    fun brukeErrorFilter_kast_riktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot Oppgave feilet.")
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

    override fun executeRequest() =
        oppgaveConsumer.hentOppgave("1")
}
