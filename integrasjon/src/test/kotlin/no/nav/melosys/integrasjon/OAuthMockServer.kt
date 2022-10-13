package no.nav.melosys.integrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.spyk
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.mock.env.MockEnvironment
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@EnableOAuth2Client
class OAuthMockServer(
    @Value("\${mockserver.security.azure.port}") mockSecurityPort: Int
) {
    private val stsMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockSecurityPort))

    @MockkBean
    private lateinit var tokenValidationContextHolder: TokenValidationContextHolder

    fun start() {
        every { tokenValidationContextHolder.tokenValidationContext } returns tokenValidationContext("sub1")
        stsMockServer.start()

        stsMockServer.stubFor(
            WireMock.get("/?grant_type=client_credentials&scope=openid").willReturn(
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

    fun tokenValidationContext(sub: String): TokenValidationContext {
        val expiry = LocalDateTime.now().atZone(ZoneId.systemDefault()).plusSeconds(60).toInstant()
        val jwt: JWT = PlainJWT(
            JWTClaimsSet.Builder()
                .subject(sub)
                .audience("thisapi")
                .issuer("someIssuer")
                .expirationTime(Date.from(expiry))
                .claim("jti", UUID.randomUUID().toString())
                .build()
        )
        val map: MutableMap<String, JwtToken> = HashMap()
        map["issuer1"] = JwtToken(jwt.serialize())
        return TokenValidationContext(map)
    }

    fun stop() {
        stsMockServer.stop()
    }
}

