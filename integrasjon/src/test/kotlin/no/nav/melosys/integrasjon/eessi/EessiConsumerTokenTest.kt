package no.nav.melosys.integrasjon.eessi

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,

        EessiConsumerImpl::class,
        GenericContextExchangeFilter::class,
        EessiConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ]
)
@ActiveProfiles("wiremock-test")
@AutoConfigureWebClient
class EessiConsumerTokenTest(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<String, List<String>>(mockServiceUnderTestPort, mockSecurityPort) {

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
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
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
        }.message.shouldContain("Authorization: Bearer --token-from-system--         <<<<< Header does not match")
    }

    override fun getMockData(): String = "[]"

    override fun executeRequest() =
        eessiConsumer.hentMuligeAksjoner("123")
}
