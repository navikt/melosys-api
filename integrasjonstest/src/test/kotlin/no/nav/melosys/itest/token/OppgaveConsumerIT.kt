package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerImpl
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,
        MockRestServerProvider::class,

        OppgaveConsumerImpl::class,
        OppgaveConsumerProducer::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class OppgaveConsumerIT(
    @Autowired private val oppgaveConsumer: OppgaveConsumer,
    @Autowired mockRestServerProvider: MockRestServerProvider,
    @Value("\${mockserver.port}") mockPort: Int,
) :ConsumerWireMockTestBase <String>(mockRestServerProvider, mockPort) {

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
    fun brukeErrorFilter_kast_riktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot Oppgave feilet.")
        }
    }

    override fun getMockData(): String {
        return "{}"
    }

    override fun executeRequest() {
        oppgaveConsumer.hentOppgave("1")
    }
}
