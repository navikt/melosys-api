package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.aareg.AaregService
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer

class AaregServiceAutoTokenIT(
    @Autowired private val aaregService: AaregService,
    @Autowired private val server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : AaregServiceTestBase(server, aaregService, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun kodeOppslag(): KodeOppslag = KodeOppslagImpl()

        @Bean
        fun unleash() = FakeUnleash().apply { enable("melosys.auto.token") }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }
}
