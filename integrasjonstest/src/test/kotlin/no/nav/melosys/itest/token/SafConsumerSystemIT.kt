package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer

class SafConsumerSystemIT(
    @Autowired @Qualifier("system") private val safConsumer: SafConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : SafConsumerTestBase(server, mockPort, safConsumer) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash() = FakeUnleash()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot SAF feilet.")
        }
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        executeRequest()
    }
}
