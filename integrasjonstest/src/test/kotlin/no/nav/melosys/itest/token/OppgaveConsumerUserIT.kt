package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer

class OppgaveConsumerUserIT(
    @Autowired private val oppgaveConsumer: OppgaveConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : OppgaveConsumerTestBase(server, mockPort, oppgaveConsumer) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash() = FakeUnleash()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SpringSubjectHandler.set(TestSubjectHandler())

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
            )
        )
        executeRequest()
    }
}
