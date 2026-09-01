package no.nav.melosys.domain.mottatteopplysninger

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import kotlin.test.Test

class MottatteOpplysningerKonvertererTest {

    @Test
    fun `skal tåle null for boolean-felt i søknad (Jackson 2-kompatibilitet)`() {
        val json = javaClass.classLoader.getResource("soeknad/soeknad.json")?.readText()
            ?: throw IllegalArgumentException("Kunne ikke lese json fra 'soeknad/soeknad.json'")

        val mapper = JsonMapper.builder().build()
        val tree = mapper.readTree(json) as ObjectNode
        (tree.get("soeknadsland") as ObjectNode).putNull("flereLandUkjentHvilke")
        val jsonMedNullBoolean = mapper.writeValueAsString(tree)

        MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            jsonData = jsonMedNullBoolean
        }.also { mottatteOpplysninger ->
            MottatteOpplysningerKonverterer.lastMottatteOpplysninger(mottatteOpplysninger)
            val soeknad = mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            soeknad.soeknadsland.isFlereLandUkjentHvilke shouldBe false
        }
    }

    @Test
    fun `skal fungere å konverte fra json til dto og tilbake til json`() {
        val json = javaClass.classLoader.getResource("soeknad/soeknad.json")?.readText()
            ?: throw IllegalArgumentException("Kunne ikke lese json fra 'soeknad/soeknad.json'")

        MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            jsonData = json
        }.also { mottatteOpplysninger ->
            MottatteOpplysningerKonverterer.lastMottatteOpplysninger(mottatteOpplysninger)

            mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            mottatteOpplysninger.jsonData = null

            MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)
            mottatteOpplysninger.jsonData shouldEqualJson json
        }
    }

    @Test
    fun `skal kunne deserialisere LocalDate fra gammelt Jackson2 array-format`() {
        // Jackson 2 med WRITE_DATES_AS_TIMESTAMPS=true lagret datoer som arrays [år,måned,dag].
        // Eksisterende databaserader har dette formatet — konvertereren må fortsatt kunne lese dem.
        val json = javaClass.classLoader.getResource("soeknad/soeknad.json")?.readText()
            ?: throw IllegalArgumentException("Kunne ikke lese json fra 'soeknad/soeknad.json'")
        val arrayJson = json
            .replace("\"2018-01-02\"", "[2018,1,2]")
            .replace("\"2019-01-02\"", "[2019,1,2]")
            .replace("\"2018-01-01\"", "[2018,1,1]")
            .replace("\"2018-06-01\"", "[2018,6,1]")

        MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            jsonData = arrayJson
        }.also { mottatteOpplysninger ->
            MottatteOpplysningerKonverterer.lastMottatteOpplysninger(mottatteOpplysninger)

            val soeknad = mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            soeknad.periode.fom shouldBe LocalDate.of(2018, 1, 2)
            soeknad.periode.tom shouldBe LocalDate.of(2019, 1, 2)
        }
    }
}
