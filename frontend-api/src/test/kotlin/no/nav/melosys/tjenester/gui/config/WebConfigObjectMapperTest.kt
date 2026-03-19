package no.nav.melosys.tjenester.gui.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.config.jackson.MelosysModule
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.time.LocalDate

class WebConfigObjectMapperTest {

    private val kodeverkService = mockk<KodeverkService>(relaxed = true)
    private val webConfig = WebConfig(mockk(), kodeverkService)

    @Test
    fun `createMvcJackson2Converter should serialize dates as ISO strings`() {
        val mapper = webConfig.createMvcJackson2Converter().objectMapper
        mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) shouldBe false
        val json = mapper.writeValueAsString(mapOf("date" to LocalDate.of(2025, 1, 15)))
        json shouldBe """{"date":"2025-01-15"}"""
    }

    @Test
    fun `createMvcJackson2Converter should not fail on unknown properties`() {
        val mapper = webConfig.createMvcJackson2Converter().objectMapper
        mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) shouldBe false
    }

    @Test
    fun `createMvcJackson2Converter should have DEFAULT_VIEW_INCLUSION enabled`() {
        val mapper = webConfig.createMvcJackson2Converter().objectMapper
        mapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION) shouldBe true
    }

    @Test
    fun `createMvcJackson2Converter should have MelosysModule registered`() {
        val converter = webConfig.createMvcJackson2Converter()
        val hasModule = converter.objectMapper.registeredModuleIds
            .any { it.toString().contains("MelosysModule") }
        hasModule shouldBe true
    }

    @Test
    fun `extendMessageConverters should insert Jackson 2 converter first`() {
        val converters = mutableListOf<org.springframework.http.converter.HttpMessageConverter<*>>()
        webConfig.extendMessageConverters(converters)
        (converters[0] is MappingJackson2HttpMessageConverter) shouldBe true
    }
}
