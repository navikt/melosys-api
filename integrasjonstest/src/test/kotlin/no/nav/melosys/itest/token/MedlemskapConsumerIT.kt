package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.finn.unleash.FakeUnleash
import no.nav.melosys.integrasjon.medl.MedlemskapRestConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import java.time.LocalDate

class MedlemskapConsumerIT(
    @Autowired private val medlemskapRestConsumer: MedlemskapRestConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : MedlemskapConsumerTestBase(server, mockPort) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun unleash() = FakeUnleash()
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        SpringSubjectHandler.set(TestSubjectHandler())

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
            )
        )
        val fom = LocalDate.now().minusDays(2)
        val tom = LocalDate.now().plusDays(2)
        val fnr = "12345678990"
        medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom)
    }
}
