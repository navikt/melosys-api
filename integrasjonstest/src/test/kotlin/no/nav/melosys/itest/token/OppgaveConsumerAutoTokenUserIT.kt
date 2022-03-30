package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import java.util.*

class OppgaveConsumerAutoTokenUserIT(
    @Autowired private val oppgaveConsumer: OppgaveConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : OppgaveConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash(): Unleash = FakeUnleash().apply { enable("melosys.auto.token") }
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
            )
        )
        oppgaveConsumer.hentOppgave("1")

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SpringSubjectHandler.set(TestSubjectHandler())

        ThreadLocalAccessInfo.beforeControllerRequest("request")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
            )
        )

        oppgaveConsumer.hentOppgave("1")

        ThreadLocalAccessInfo.afterControllerRequest("request")
    }
}
