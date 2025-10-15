package no.nav.melosys.domain.dokument.felles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tester for Land-klassen som verifiserer Jackson-serialisering/deserialisering.
 *
 * Disse testene sikrer at Land kan håndtere to JSON-formater:
 * 1. Kompakt string-format: "NOR" (brukt i testdata)
 * 2. Fullt objekt-format: {"kode":"NOR"} (brukt i database-lagring)
 *
 * VIKTIG: Hvis @JsonCreator fjernes fra Land.av() vil følgende test feile:
 * - deserialiserer fra string-verdi til Land-objekt
 * Med feilmelding: "Cannot construct instance of Land: no String-argument constructor/factory method"
 *
 * VIKTIG: Hvis @JsonValue legges til på hentKode() vil databasen få NULL-verdier
 * fordi Land serialiseres som bare "NOR" i stedet for {"kode":"NOR"}
 */
class LandTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `deserialiserer fra string-verdi til Land-objekt`() {
        // Dette formatet brukes i testdata hvor Land er representert som en enkel string
        val json = "\"NOR\""

        val land: Land = objectMapper.readValue(json)

        land.kode shouldBe "NOR"
    }

    @Test
    fun `deserialiserer fra objekt med kode-felt til Land-objekt`() {
        // Dette formatet brukes når hele objektet serialiseres til database
        val json = """{"kode":"NOR"}"""

        val land: Land = objectMapper.readValue(json)

        land.kode shouldBe "NOR"
    }

    @Test
    fun `serialiserer Land-objekt til JSON med kode-felt`() {
        // Når vi serialiserer til database må vi få et fullt JSON-objekt
        val land = Land("NOR")

        val json = objectMapper.writeValueAsString(land)

        json shouldBe """{"kode":"NOR"}"""
    }

    @Test
    fun `round-trip serialisering og deserialisering bevarer data`() {
        // Sikrer at vi kan lagre og hente Land fra database uten tap av data
        val original = Land("DNK")

        val json = objectMapper.writeValueAsString(original)
        val deserialisert: Land = objectMapper.readValue(json)

        deserialisert.kode shouldBe original.kode
        deserialisert.hentKode() shouldBe original.hentKode()
    }

    @Test
    fun `factory-metode av() fungerer som forventet`() {
        val land = Land.av("FIN")

        land.kode shouldBe "FIN"
    }

    @Test
    fun `factory-metode av() håndterer null`() {
        val land = Land.av(null)

        land.kode shouldBe null
    }

    @Test
    fun `toString returnerer landkode`() {
        val land = Land("ISL")

        land.toString() shouldBe "ISL"
    }
}
