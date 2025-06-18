package no.nav.melosys.itest

import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor.Companion.API_KEY_HEADER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
class SatsendringAdminControllerIT(
    @Autowired private val mockMvc: MockMvc
) : ComponentTestBase() {

    private val testYear = 2025

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
        ).andExpect(MockMvcResultMatchers.status().isAccepted)
    }
}
