package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.integrasjon.joark.saf.SafConsumerImpl
import no.nav.melosys.integrasjon.joark.saf.SafConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,
        MockRestServerProvider::class,

        SafConsumerImpl::class,
        SafConsumerProducer::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class SafConsumerIT(
    @Autowired private val safConsumer: SafConsumer,
    @Autowired mockRestServerProvider: MockRestServerProvider,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerWireMockTestBase<ByteArray>(mockRestServerProvider, mockPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
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
                    Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        executeRequest()
    }

    @Test
    fun authorizationSkalKommeFraBruker_Feiler_nårUtenSubjectHandler() {
        ThreadLocalAccessInfo.beforeControllerRequest("request", false)
        SpringSubjectHandler.set(NullSubjectHandler())

        AssertionsForClassTypes.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { executeRequest() }
            .withMessageContaining("Token mangler fra bruker! ThreadLocalAccessInfo{requestUri='request', prossessId='null'}")

        ThreadLocalAccessInfo.afterControllerRequest("request")
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot SAF feilet.")
        }
    }

    override fun getMockData(): ByteArray {
        return ByteArray(0)
    }

    override fun executeRequest() {
        safConsumer.hentDokument("1", "1")
    }
}
