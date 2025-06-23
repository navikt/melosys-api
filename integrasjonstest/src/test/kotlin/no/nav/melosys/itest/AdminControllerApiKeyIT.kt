package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.Application
import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor.Companion.API_KEY_HEADER
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@AutoConfigureMockMvc
class AdminControllerApiKeyIT(
    @Autowired var mockMvc: MockMvc
) : OracleTestContainerBase() {
    @Test
    fun `should return 403 when API key is missing`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .accept(MediaType.APPLICATION_JSON)

        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `should return 403 when API key is incorrect`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, "incorrect")
                .accept(MediaType.APPLICATION_JSON)

        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `should return 200 when API key is correct`() {

        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, "dummy")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
    }
}
