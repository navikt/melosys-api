package no.nav.melosys.itest

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import jakarta.servlet.ServletException
import no.nav.melosys.integrasjon.kafka.KafkaContainerService
import no.nav.melosys.integrasjon.kafka.KafkaErrorController
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler
import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor
import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor.Companion.API_KEY_HEADER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.ConcurrentHashMap

@WebMvcTest(controllers = [KafkaErrorController::class])
@Import(AdminControllerApiKeyTest.ApiKeyTestConfig::class, KafkaErrorController::class)
@ActiveProfiles("test")
@ExtendWith(MockKExtension::class)
class AdminControllerApiKeyTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var kafkaContainerService: KafkaContainerService

    @MockkBean
    lateinit var skippableKafkaErrorHandler: SkippableKafkaErrorHandler

    @Test
    fun `should throw`() {
        shouldThrow<ServletException> {
            mockMvc.perform(
                get("/admin/kafka/errors")
                    .accept(MediaType.APPLICATION_JSON)
            )
        }.rootCause.message shouldBe "Trenger gyldig apikey"
    }

    @Test
    fun `should return 200 when API key is correct`() {
        every { skippableKafkaErrorHandler.failedMessages } returns ConcurrentHashMap<String, SkippableKafkaErrorHandler.Failed>()

        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, "Dummy")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
    }

    @Configuration
    class ApiKeyTestConfig {
        @Bean
        fun apiKeyInterceptor() = ApiKeyInterceptor("Dummy")

        @Bean
        fun webMvcConfigurer(apiKeyInterceptor: ApiKeyInterceptor): WebMvcConfigurer =
            object : WebMvcConfigurer {
                override fun addInterceptors(registry: InterceptorRegistry) {
                    registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/admin/**")
                }
            }
    }
}
