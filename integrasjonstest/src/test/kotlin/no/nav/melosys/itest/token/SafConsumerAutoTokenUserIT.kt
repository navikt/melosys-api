package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import java.util.*

class SafConsumerAutoTokenUserIT(
    @Autowired private val safConsumer: SafConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : SafConsumerTestBase(server, mockPort) {

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
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        safConsumer.hentDokument("1", "1")

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SpringSubjectHandler.set(TestSubjectHandler())
        ThreadLocalAccessInfo.beforeControllerRequest("request")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        safConsumer.hentDokument("1", "1")

        ThreadLocalAccessInfo.afterControllerRequest("request")
    }
}
