package no.nav.melosys.integrasjon.joark.saf

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,

        SafConsumerImpl::class,
        SafConsumerProducer::class,
        GenericContextExchangeFilter::class
    ]
)
@ActiveProfiles("wiremock-test")
@AutoConfigureWebClient
class SafConsumerTokenTest(
    @Autowired private val safConsumer: SafConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<ByteArray,ByteArray>(mockServiceUnderTestPort, mockSecurityPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        executeFromController()
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

    @Test
    fun correlationIdLeggesPåRequest() {
        verifyHeaders(
            mapOf(
                Pair("X-Correlation-ID", WireMock.matching(UUID_REGEX)),
            )
        )
        executeRequest()
    }

    override fun getMockData(): ByteArray {
        return ByteArray(0)
    }

    override fun executeRequest() = safConsumer.hentDokument("1", "1")
}
