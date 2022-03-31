package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.pdl.PDLConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import java.util.*

class PDLConsumerAutoTokenSystemIT(
    @Autowired @Qualifier("system") private val pdlConsumer: PDLConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : PDLConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash() = FakeUnleash().apply { enable("melosys.auto.token") }
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )
        pdlConsumer.hentIdenter("0")

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SpringSubjectHandler.set(TestSubjectHandler())

        ThreadLocalAccessInfo.beforeControllerRequest("request")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )

        pdlConsumer.hentIdenter("0")

        ThreadLocalAccessInfo.afterControllerRequest("request")
    }
}
