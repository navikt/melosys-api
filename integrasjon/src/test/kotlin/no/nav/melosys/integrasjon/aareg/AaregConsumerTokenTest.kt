package no.nav.melosys.integrasjon.aareg

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.*
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@TestConfiguration
class UnleashTestConfig {
    @Bean
    fun unleash(): Unleash {
        val fakeUnleash = FakeUnleash()
        fakeUnleash.enable(ToggleName.MELOSYS_AAREG_AZURE)
        return fakeUnleash
    }
}

@Import(
    StsWebClientProducer::class,
    SecurityTokenServiceConsumer::class,
    RestSTSService::class,
    OAuthMockServer::class,

    ArbeidsforholdConsumerConfig::class,
    GenericAuthFilterFactory::class,
    StsAuthExchangeFilter::class,
    UnleashTestConfig::class
)
@WebMvcTest
@ActiveProfiles("wiremock-test")
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
