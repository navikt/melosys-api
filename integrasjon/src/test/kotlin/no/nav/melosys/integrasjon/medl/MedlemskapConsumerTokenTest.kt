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
import java.time.LocalDate

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,

        MedlemskapRestConsumer::class,
        MedlemskapRestConsumerProducer::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:token-test"]
)
@AutoConfigureWebClient
class MedlemskapConsumerTokenTest(
    @Autowired private val medlemskapRestConsumer: MedlemskapRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<String>(mockServiceUnderTestPort, mockSecurityPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
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
                )
            )
        }
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
