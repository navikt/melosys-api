package no.nav.melosys.tjenester.gui.config

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WebConfigObjectMapperTest {

    private val kodeverkService = mockk<KodeverkService>(relaxed = true)
    private val webConfig = WebConfig(mockk())
    private val objectMapper = webConfig.objectMapper(kodeverkService)

    @Test
    fun `objectMapper should be a JsonMapper instance`() {
        objectMapper.shouldBeInstanceOf<JsonMapper>()
    }

    @Test
    fun `objectMapper should serialize dates as ISO strings, not timestamps`() {
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
}
