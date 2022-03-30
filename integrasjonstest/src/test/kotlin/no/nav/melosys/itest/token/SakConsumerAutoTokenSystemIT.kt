package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.nav.melosys.integrasjon.sak.SakConsumer
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

class SakConsumerAutoTokenSystemIT(
    @Autowired @Qualifier("system") private val  sakConsumer: SakConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : SakConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash(): Unleash {
            return FakeUnleash().apply { enable("melosys.auto.token") }
        }
    }

    @Test
    fun authorizationSkalKommeFraProsess() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Basic dGVzdDp0ZXN0")),
            )
        )
        sakConsumer.hentSak(1L)

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun authorizationSkalKommeFraWeb() {
        SpringSubjectHandler.set(TestSubjectHandler())
        ThreadLocalAccessInfo.preHandle("request")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
            )
        )
        sakConsumer.hentSak(1L)

        ThreadLocalAccessInfo.afterCompletion("request")
    }

}
