package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.eessi.EessiConsumer
import no.nav.melosys.integrasjon.eessi.EessiConsumerImpl
import no.nav.melosys.integrasjon.eessi.EessiConsumerProducer
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,

        EessiConsumerImpl::class,
        EessiConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
@AutoConfigureWebClient
class EessiConsumerIT(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityUrl: Int
) : ConsumerTestBase<String>(mockServiceUnderTestPort, mockSecurityUrl) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
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

    override fun errorFromServerMessage() = "500 Server Error: \"{\"melding\": \"Internal Server Error\"}\""
    override fun getMockData(): String = "[]"

    override fun executeRequest() {
        eessiConsumer.hentMuligeAksjoner("123")
    }
}

