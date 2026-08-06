package no.nav.melosys.tjenester.gui.config

import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.json.JsonMapper
import io.kotest.assertions.throwables.shouldThrow
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
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class WebConfigObjectMapperTest {

    private val kodeverkService = mockk<KodeverkService>(relaxed = true)
    private val webConfig = WebConfig(mockk()) { objectMapper }
    private val objectMapper: JsonMapper = run {
        val builder = JsonMapper.builder()
        webConfig.melosysJsonMapperCustomizer(kodeverkService).customize(builder)
        builder.build()
    }

    /** Converterne configureMessageConverters faktisk setter opp, bygget med Springs ekte builder. */
    private val mvcConvertere: List<HttpMessageConverter<*>> = run {
        val builder = HttpMessageConverters.forServer().registerDefaults()
        webConfig.configureMessageConverters(builder)
        builder.build().toList()
    }

    /**
     * Mapperen configureMessageConverters bygger. NB: basen her er bygget kun av vår egen customizer, ikke av
     * Boot, så denne verifiserer coercion-reglene og at rebuild() bevarer basen - ikke hele produksjonskjeden.
     * Den ekte kjeden er dekket ende-til-ende av @WebMvcTest-en i ValideringUnntaksperiodeControllerTest.
     */
    private val mvcMapper: JsonMapper =
        mvcConvertere.filterIsInstance<JacksonJsonHttpMessageConverter>().single().mapper

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

    @Test
    fun `mvcMapper skal avvise tall som LocalDate i stedet for å tolke det som epoch-day`() {
        shouldThrow<DatabindException> {
            mvcMapper.readValue("""{"dato": 12345}""", DatoDto::class.java)
        }
    }

    @Test
    fun `mvcMapper skal avvise tall som LocalDateTime i stedet for å tolke det som timestamp`() {
        shouldThrow<DatabindException> {
            mvcMapper.readValue("""{"tidspunkt": 12345}""", TidspunktDto::class.java)
        }
    }

    @Test
    fun `mvcMapper skal fortsatt lese datoer på ISO-format`() {
        mvcMapper.readValue("""{"dato": "2025-01-15"}""", DatoDto::class.java).dato shouldBe LocalDate.of(2025, 1, 15)
        mvcMapper.readValue("""{"tidspunkt": "2025-01-15T10:30:00"}""", TidspunktDto::class.java)
            .tidspunkt shouldBe LocalDateTime.of(2025, 1, 15, 10, 30, 0)
    }

    @Test
    fun `configureMessageConverters skal fjerne Jackson-convertere for andre formater enn JSON`() {
        // De har egne mappere uten coercion-reglene, og ville dermed vært en vei rundt datovernet
        mvcConvertere.filterIsInstance<AbstractJacksonHttpMessageConverter<*>>()
            .map { it::class.java } shouldBe listOf(JacksonJsonHttpMessageConverter::class.java)
        // Ikke-Jackson-converterne (String, Resource, ByteArray) skal fortsatt være der - PDF-nedlasting
        // og lignende avhenger av dem
        mvcConvertere.any { it is StringHttpMessageConverter } shouldBe true
    }

    @Test
    fun `mvcMapper skal fortsatt godta tall som Instant, siden epoch-tid er en gyldig representasjon der`() {
        // Til forskjell fra epoch-day for LocalDate er et tall for Instant veldefinert. Å avvise det ville
        // brutt gyldige requester i stedet for å fange en feil.
        mvcMapper.readValue("""{"tidspunkt": 12345}""", InstantDto::class.java).tidspunkt shouldNotBe null
    }

    @Test
    fun `mvcMapper skal beholde konfigurasjonen fra mapperen den bygges videre fra`() {
        // rebuild() skal bevare moduler og features, slik at MVC ikke drifter fra resten av appen
        mvcMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION) shouldBe true
        mvcMapper.writeValueAsString(InnvilgelsesResultat.INNVILGET) shouldBe "\"${InnvilgelsesResultat.INNVILGET.kode}\""
    }

    @Test
    fun `coercion-reglene skal IKKE gjelde den delte mapperen som Kafka og WebClient bruker`() {
        // Kafka-consumerne deler denne mapperen, og en deserialiseringsfeil der stopper containeren.
        // Coercion-reglene hører derfor kun hjemme på MVC-mapperen.
        objectMapper.readValue("""{"dato": 12345}""", DatoDto::class.java).dato shouldNotBe null
    }

    data class DatoDto(val dato: LocalDate?)

    data class InstantDto(val tidspunkt: Instant?)

    data class TidspunktDto(val tidspunkt: LocalDateTime?)
}
