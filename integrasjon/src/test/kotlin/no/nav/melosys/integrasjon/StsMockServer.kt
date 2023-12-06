package no.nav.melosys.integrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.spyk
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.mock.env.MockEnvironment

@Import(SecurityTokenServiceConsumer::class)
class StsMockServer(
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) {
    private val stsMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockSecurityPort))

    fun start() {
        stsMockServer.start()

        stsMockServer.stubFor(
            WireMock.get("/token?grant_type=client_credentials&scope=openid").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"access_token\": \"--token-from-system--\", \"expires_in\": \"123\" }")
            )
        )

        val environment = spyk(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    fun stop() {
        stsMockServer.stop()
    }
}

