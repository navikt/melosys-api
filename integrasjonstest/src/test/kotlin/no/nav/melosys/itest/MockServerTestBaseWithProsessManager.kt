package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.Extension
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.melosysmock.oppgave.OppgaveRepo
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class MockServerTestBaseWithProsessManager(
    extensionForWireMock: Extension? = null
) : ComponentTestBase() {

    @Autowired
    protected lateinit var oppgaveRepo: OppgaveRepo

    @Autowired
    protected lateinit var prosessinstansTestManager: ProsessinstansTestManager

    private val randomUUID = UUID.randomUUID()

    protected val mockServer: WireMockServer =
        WireMockServer(
            if (extensionForWireMock == null) WireMockConfiguration.options().port(8094) else
                WireMockConfiguration
                    .options().extensions(extensionForWireMock)
                    .port(8094)
        )

    @BeforeEach
    fun beforeComponentTestBaseWithMockServer() {
        // Setter opp systemkontekst slik at kall fra testene ikke logger "Call have not been registrert from RestController or Prosess"
        ThreadLocalAccessInfo.beforeExecuteProcess(randomUUID, "steg")
        mockServer.start()
        mockServer.stubFor(
            WireMock.post("/api/inngangsvilkaar").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"kvalifisererForEf883_2004\" : false, \"feilmeldinger\" : [] }")
            )
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching("/api/v1/mal/.*/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .withQueryParam("utkast", equalTo("false"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ByteArray(0))
                )
        )
    }

    @AfterEach
    fun afterComponentTestBaseWithMockServer() {
        oppgaveRepo.repo.clear()
        prosessinstansTestManager.clear()
        ThreadLocalAccessInfo.afterExecuteProcess(randomUUID)
        mockServer.stop()
    }
}
