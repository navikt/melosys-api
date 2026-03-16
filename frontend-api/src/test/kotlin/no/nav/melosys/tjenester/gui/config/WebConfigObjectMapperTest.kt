package no.nav.melosys.tjenester.gui.config

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.dto.BehandlingOppsummeringDto
import org.junit.jupiter.api.Test
import java.time.Instant
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
    fun `objectMapper should serialize Instant as ISO-8601 string, not timestamp`() {
        val instant = Instant.parse("2025-01-15T10:30:00Z")
        val json = objectMapper.writeValueAsString(mapOf("ts" to instant))
        json shouldMatch Regex("""^\{"ts":"2025-01-15T10:30:00(\.\d+)?Z"\}$""")
    }

    @Test
    fun `BehandlingOppsummeringDto skal serialisere Instant og LocalDate som ISO-8601 strings`() {
        val dto = BehandlingOppsummeringDto().apply {
            registrertDato = Instant.parse("2025-03-13T10:00:00Z")
            behandlingsfrist = LocalDate.of(2025, 6, 1)
        }

        val tree = objectMapper.readTree(objectMapper.writeValueAsString(dto))

        tree["registrertDato"].asText() shouldMatch Regex("""2025-03-13T10:00:00(\.\d+)?Z""")
        tree["behandlingsfrist"].asText() shouldBe "2025-06-01"
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
    fun `KodeSerializer serialiserer Kodeverk i IKKE_MAPPES_TIL_KODE_DTO som plain string`() {
        // InnvilgelsesResultat er i IKKE_MAPPES_TIL_KODE_DTO og skal serialiseres som plain string.
        // I Jackson 3 må modulens serializer fortsatt prioriteres fremfor default enum-serialisering.
        val json = objectMapper.writeValueAsString(InnvilgelsesResultat.INNVILGET)

        json shouldStartWith "\""
        json shouldBe "\"${InnvilgelsesResultat.INNVILGET.kode}\""
    }

    @Test
    fun `KodeSerializer serialiserer Kodeverk utenfor IKKE_MAPPES_TIL_KODE_DTO som KodeDto-objekt`() {
        // Sakstyper er IKKE i IKKE_MAPPES_TIL_KODE_DTO og skal serialiseres som {"kode":"...","term":"..."}.
        // Verifiserer at MelosysModule sin KodeSerializer fortsatt brukes for disse i Jackson 3.
        val node = objectMapper.readTree(objectMapper.writeValueAsString(Sakstyper.EU_EOS))

        node["kode"] shouldNotBe null
        node["term"] shouldNotBe null
        node["kode"].asText() shouldBe Sakstyper.EU_EOS.kode
    }
}
