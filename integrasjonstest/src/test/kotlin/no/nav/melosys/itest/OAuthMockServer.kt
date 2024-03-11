package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.beans.factory.annotation.Value

@EnableOAuth2Client
class OAuthMockServer(
    @Value("\${mockserver.security.azure.port}") mockSecurityPort: Int
) {
    private val azureMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockSecurityPort))


    fun start() {
        azureMockServer.start()

        azureMockServer.stubFor(
            WireMock.post("/oauth2/v2.0/token").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """ {
                        "token_type": "Bearer",
                        "scope": "scope1 scope2",
                        "expires_in": 3952,
                        "ext_expires_in": 3952,
                        "access_token": "-- user_access_token -- "
                        }
                    """
                    )
            )
        )
    }

    fun stop() {
        azureMockServer.stop()
    }
}

