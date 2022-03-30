package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer

class SafConsumerUserIT(
    @Autowired private val safConsumer: SafConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : SafConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash(): Unleash = FakeUnleash()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SpringSubjectHandler.set(TestSubjectHandler())

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        safConsumer.hentDokument("1", "1")
    }

    @Test
    fun authorizationSkalKommeFraBruker_Feiler_nårUtenSubjectHandler() {
        SpringSubjectHandler.set(NullSubjectHandler())

        AssertionsForClassTypes.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { safConsumer.hentDokument("1", "1") }
            .withMessageContaining("Token mangler!")

    }
}
