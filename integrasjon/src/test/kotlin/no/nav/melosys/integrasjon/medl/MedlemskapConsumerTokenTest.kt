package no.nav.melosys.integrasjon.medl

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,

        MedlemskapRestConsumer::class,
        MedlemskapRestConsumerProducer::class,
        GenericContextExchangeFilter::class
    ]
)
@ActiveProfiles("wiremock-test")
@AutoConfigureWebClient
class MedlemskapConsumerTokenTest(
    @Autowired private val medlemskapRestConsumer: MedlemskapRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<String, Unit>(mockServiceUnderTestPort, mockSecurityPort) {

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
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
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
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot Medl feilet.")
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

    override fun executeRequest() {
        val fom = LocalDate.now().minusDays(2)
        val tom = LocalDate.now().plusDays(2)
        val fnr = "12345678990"
        medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom)
    }
}
