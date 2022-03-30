package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
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

class PDLConsumerAutoTokenUserIT(
    @Autowired @Qualifier("saksbehandler") private val pdlConsumer: PDLConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : PDLConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        val unleash = FakeUnleash()

        @Bean
        fun unleash(): Unleash {
            unleash.enable("melosys.auto.token")
            return unleash
        }
    }

    @Test
    fun testRequestFromProsess() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )
        pdlConsumer.hentIdenter("99026522600")

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun testRequestFraWeb() {
        SpringSubjectHandler.set(TestSubjectHandler())

        ThreadLocalAccessInfo.preHandle("request")

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )

        pdlConsumer.hentIdenter("99026522600")

        ThreadLocalAccessInfo.afterCompletion("request")
    }
}
