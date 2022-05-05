package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.client.MockRestServiceServer

class SafConsumerIT(
    @Autowired private val safConsumer: SafConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : SafConsumerTestBase(server, mockPort, safConsumer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                    Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Id", WireMock.equalTo("melosys"))
            )
        )
        executeRequest()
    }

    @Test
    fun authorizationSkalKommeFraBruker_Feiler_nårUtenSubjectHandler() {
        ThreadLocalAccessInfo.beforeControllerRequest("request", false)
        SpringSubjectHandler.set(NullSubjectHandler())

        AssertionsForClassTypes.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { executeRequest() }
            .withMessageContaining("Token mangler fra bruker! ThreadLocalAccessInfo{requestUri='request', prossessId='null'}")

        ThreadLocalAccessInfo.afterControllerRequest("request")
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot SAF feilet.")
        }
    }
}
