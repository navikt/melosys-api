package no.nav.melosys.integrasjon.popp

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.LocalDateTime

/**
 * Verifiserer at PensjonsopptjeningHendelse serialiseres korrekt for Kafka-produksjon.
 *
 * Bruker Spring Boot sin auto-konfigurerte ObjectMapper (JavaTimeModule + KotlinModule,
 * uten MelosysModule) — identisk med ObjectMapper som KafkaConfig injiserer i produksjon.
 */
@JsonTest
class PensjonsopptjeningHendelseKafkaSerializationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val hendelse = PensjonsopptjeningHendelse(
        hendelsesId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        fnr = "12345678901",
        pgi = 500_000L,
        inntektsAr = 2023,
        fastsattTidspunkt = LocalDateTime.of(2024, 3, 15, 10, 30, 0),
        endringstype = PensjonsopptjeningHendelse.Endringstype.NY_INNTEKT,
        melosysBehandlingID = 42L
    )

    @Test
    fun `PensjonsopptjeningHendelse serialiserer alle felter korrekt`() {
        val json = objectMapper.writeValueAsString(hendelse)

        json shouldEqualJson """
            {
                "hendelsesId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "fnr": "12345678901",
                "pgi": 500000,
                "inntektsAr": 2023,
                "fastsattTidspunkt": "2024-03-15T10:30:00",
                "endringstype": "NY_INNTEKT",
                "melosysBehandlingID": 42
            }
        """
    }

    @Test
    fun `PensjonsopptjeningHendelse serialiserer dato som ISO-streng`() {
        val json = objectMapper.writeValueAsString(hendelse)

        json.shouldContainJsonKeyValue("$.fastsattTidspunkt", "2024-03-15T10:30:00")
    }

    @Test
    fun `PensjonsopptjeningHendelse inneholder ikke kode-term-objekter`() {
        val json = objectMapper.writeValueAsString(hendelse)

        json shouldNotContain """"kode""""
        json shouldNotContain """"term""""
    }

    @Test
    fun `PensjonsopptjeningHendelse round-trip deserialisering`() {
        val json = objectMapper.writeValueAsString(hendelse)
        val deserialized = objectMapper.readValue(json, PensjonsopptjeningHendelse::class.java)

        deserialized shouldBe hendelse
    }
}
