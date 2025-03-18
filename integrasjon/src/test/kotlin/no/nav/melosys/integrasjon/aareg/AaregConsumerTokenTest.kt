package no.nav.melosys.integrasjon.aareg

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumerConfig
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdQuery
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdResponse
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        ArbeidsforholdConsumerConfig::class,
        GenericAuthFilterFactory::class,

    ]
)
@AutoConfigureWebClient
private class AaregConsumerTokenTest(
    @Autowired private val arbeidsforholdConsumer: ArbeidsforholdConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, ArbeidsforholdResponse>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraBruker() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer -- user_access_token --")),
                Pair("Nav-Consumer-Token", WireMock.absent())
            )
        )
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("srvmelosys"))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("srvmelosys"))
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            assertThat(error).startsWith("Henting av arbeidsforhold fra Aareg feilet")
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
        return "[]"
    }

    override fun executeRequest() =
        arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker("121", ArbeidsforholdQuery())
}
