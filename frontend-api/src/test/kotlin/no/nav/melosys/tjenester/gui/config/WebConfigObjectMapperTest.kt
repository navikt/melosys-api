package no.nav.melosys.tjenester.gui.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.time.LocalDate

class WebConfigObjectMapperTest {

    private val kodeverkService = mockk<KodeverkService>(relaxed = true)
    private val webConfig = WebConfig(mockk(), kodeverkService)
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val customizer = webConfig.jacksonCustomizer()
        val builder = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder()
        customizer.customize(builder)
        objectMapper = builder.build()
    }

    @Test
    fun `objectMapper should serialize dates as ISO strings, not timestamps`() {
        objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) shouldBe false

        val json = objectMapper.writeValueAsString(mapOf("date" to LocalDate.of(2025, 1, 15)))
        json shouldBe """{"date":"2025-01-15"}"""
    }

    @Test
    fun `objectMapper should not fail on unknown properties`() {
        objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) shouldBe false
    }

    @Test
    fun `objectMapper should have DEFAULT_VIEW_INCLUSION enabled`() {
        objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION) shouldBe true
    }

    @Test
    fun `objectMapper should handle Kotlin data classes`() {
        data class TestDto(val name: String, val value: Int)

        val dto = TestDto("test", 42)
        val json = objectMapper.writeValueAsString(dto)
        val deserialized = objectMapper.readValue(json, TestDto::class.java)

        deserialized shouldBe dto
    }

    @Test
    fun `extendMessageConverters should register MelosysModule on MVC converters`() {
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        val converters: MutableList<HttpMessageConverter<*>> = mutableListOf(converter)
        webConfig.extendMessageConverters(converters)
        val registeredModuleIds = converter.objectMapper.registeredModuleIds
        registeredModuleIds.any { it.toString().contains("MelosysModule") } shouldBe true
    }
}
