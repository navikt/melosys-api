package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdContextExchangeFilter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdQuery
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumerConfig
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,
        MockRestServerProvider::class,

        ArbeidsforholdRestConsumer::class,
        ArbeidsforholdRestConsumerConfig::class,
        ArbeidsforholdContextExchangeFilter::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class AaregConsumerIT(
    @Autowired private val arbeidsforholdRestConsumer: ArbeidsforholdRestConsumer,
    @Autowired mockRestServerProvider: MockRestServerProvider,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerWireMockTestBase<String>(mockRestServerProvider, mockPort) {

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

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
            assertThat(error).startsWith("Henting av arbeidsforhold fra Aareg feilet")
        }
    }

    override fun getMockData(): String {
        return "[]"
    }

    override fun executeRequest() {
        val build = ArbeidsforholdQuery.Builder().build()
        arbeidsforholdRestConsumer.finnArbeidsforholdPrArbeidstaker("121", build)
    }
}
