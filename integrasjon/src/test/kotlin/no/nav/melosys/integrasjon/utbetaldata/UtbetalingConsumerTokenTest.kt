package no.nav.melosys.integrasjon.utbetaldata

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.melosys.integrasjon.utbetaling.Periode
import no.nav.melosys.integrasjon.utbetaling.UtbetaldataRestConsumer
import no.nav.melosys.integrasjon.utbetaling.UtbetalingConsumerProducerV2
import no.nav.melosys.integrasjon.utbetaling.UtbetalingRequest
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
import java.time.LocalDate

@Import(
    StsWebClientProducer::class,
    SecurityTokenServiceConsumer::class,
    OAuthMockServer::class,

    GenericAuthFilterFactory::class,
    UtbetalingConsumerProducerV2::class,
)
@WebMvcTest
@AutoConfigureWebClient
@EnableOAuth2Client
@ActiveProfiles("wiremock-test")
class UtbetalingConsumerTokenTest(
    @Autowired private val utbetalingRestConsumer: UtbetaldataRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, Unit>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot Utbetalinger feilet.")
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

    override fun createWireMock(): MappingBuilder {
        return WireMock.post(UrlPattern.ANY)
    }

    override fun getMockData(): String {
        return "[]"
    }

    override fun executeRequest() {
        val fnr = "12345678990"
        val periodeFom = Periode(LocalDate.now().minusDays(2).toString(), LocalDate.now().plusDays(2).toString())
        utbetalingRestConsumer.hentUtbetalingsInformasjon(
            UtbetalingRequest(
                fnr,
                periodeFom,
                "UTBETALINGSPERIODE",
                "RETTIGHETSHAVER"
            )
        )
    }
}
