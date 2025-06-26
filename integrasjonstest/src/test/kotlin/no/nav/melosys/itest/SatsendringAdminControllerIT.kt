package no.nav.melosys.itest

import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor.Companion.API_KEY_HEADER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import wiremock.com.google.common.net.HttpHeaders.AUTHORIZATION

@AutoConfigureMockMvc
class SatsendringAdminControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired var mockOAuth2Server: MockOAuth2Server
) : ComponentTestBase() {

    private val testYear = 2025

    private fun hentBearerToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-oid",
                "azp" to "test-azp",
                "NAVident" to "test123"
            )
        ).serialize()
    }

    @BeforeEach
    fun setup() {
    }

    @AfterEach
    fun afterEach() {
    }

    @Test
    fun `endpoint POST admin-satsendringer-år skal returnere 202 Accepted`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/satsendringer/${testYear}")
                .header(API_KEY_HEADER, "dummy")
                .header(AUTHORIZATION, "Bearer ${hentBearerToken()}")
        ).andExpect(MockMvcResultMatchers.status().isAccepted)
    }
}
