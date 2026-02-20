package no.nav.melosys.integrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@EnableOAuth2Client
class OAuthMockServer(
    @Value("\${mockserver.security.azure.port}") mockSecurityPort: Int
) {
    private val azureMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockSecurityPort))

    private val tokenValidationContextHolder: TokenValidationContextHolder = mockk(relaxed = true)

    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder = tokenValidationContextHolder

    private fun shouldUseSystemToken() = ThreadLocalAccessInfo.shouldUseSystemToken()

    private fun getToken(): String = if (shouldUseSystemToken()) "--azure-token-from-system--" else "-- user_access_token --"

    fun reset() {
        clearMocks(tokenValidationContextHolder)
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            verify(exactly = 0) { tokenValidationContextHolder.getTokenValidationContext() }
            verify(exactly = 0) { tokenValidationContextHolder.setTokenValidationContext(any()) }
        } else {
            every { tokenValidationContextHolder.getTokenValidationContext() } returns tokenValidationContext()
            every { tokenValidationContextHolder.setTokenValidationContext(any()) } returns Unit
        }
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
                        "access_token": "${getToken()}"
                        }
                    """
                    )
            )
        )
    }

    fun start() {
        azureMockServer.start()
    }

    fun stop() {
        azureMockServer.stop()
    }

    private fun tokenValidationContext(): TokenValidationContext {
        val expiry = LocalDateTime.now().atZone(ZoneId.systemDefault()).plusSeconds(60).toInstant()
        val jwt: JWT = PlainJWT(
            JWTClaimsSet.Builder()
                .subject("sub1")
                .audience("thisapi")
                .issuer("someIssuer")
                .expirationTime(Date.from(expiry))
                .claim("jti", UUID.randomUUID().toString())
                .build()
        )
        return TokenValidationContext(mapOf("issuer1" to JwtToken(jwt.serialize())))
    }
}

