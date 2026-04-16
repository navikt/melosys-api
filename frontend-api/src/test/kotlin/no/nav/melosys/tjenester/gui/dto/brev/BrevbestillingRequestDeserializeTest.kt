package no.nav.melosys.tjenester.gui.dto.brev

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

/**
 * Verifiserer at BrevbestillingRequest kan deserialiseres fra JSON der boolean-felt
 * er null eller mangler. Etter Jackson 2→3 migreringen (MELOSYS-7948) feiler
 * deserialisering av null til primitiv boolean som standard.
 *
 * Reproduserer prodfeilen: "Cannot map `null` into type `boolean`"
 */
class BrevbestillingRequestDeserializeTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `deserialiserer request med null boolean-felt uten feil`() {
        val json = """
            {
              "produserbardokument": null,
              "mottaker": null,
              "skalViseStandardTekstOmkontaktopplysninger": null,
              "skalViseStandardTekstOmOpplysninger": null,
              "erInnvilgelse": null,
              "erEøsPensjonist": null,
              "erEøsLovvalg": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<BrevbestillingRequest>(json)

        dto.skalViseStandardTekstOmkontaktopplysninger().shouldBeNull()
        dto.skalViseStandardTekstOmOpplysninger().shouldBeNull()
        dto.erInnvilgelse().shouldBeNull()
        dto.erEøsPensjonist().shouldBeNull()
        dto.erEøsLovvalg().shouldBeNull()
    }

    @Test
    fun `deserialiserer request der boolean-felt mangler i JSON`() {
        val json = """
            {
              "produserbardokument": null,
              "mottaker": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<BrevbestillingRequest>(json)

        dto.erEøsPensjonist().shouldBeNull()
        dto.erEøsLovvalg().shouldBeNull()
        dto.erInnvilgelse().shouldBeNull()
    }

    @Test
    fun `deserialiserer request med eksplisitte boolean-verdier`() {
        val json = """
            {
              "produserbardokument": null,
              "mottaker": null,
              "skalViseStandardTekstOmkontaktopplysninger": true,
              "skalViseStandardTekstOmOpplysninger": false,
              "erInnvilgelse": true,
              "erEøsPensjonist": false,
              "erEøsLovvalg": true
            }
        """.trimIndent()

        val dto = objectMapper.readValue<BrevbestillingRequest>(json)

        dto.skalViseStandardTekstOmkontaktopplysninger() shouldBe true
        dto.skalViseStandardTekstOmOpplysninger() shouldBe false
        dto.erInnvilgelse() shouldBe true
        dto.erEøsPensjonist() shouldBe false
        dto.erEøsLovvalg() shouldBe true
    }

    @Test
    fun `tilUtkast konverterer null boolean til false uten NPE`() {
        val json = """
            {
              "produserbardokument": null,
              "mottaker": null,
              "skalViseStandardTekstOmkontaktopplysninger": null,
              "skalViseStandardTekstOmOpplysninger": null
            }
        """.trimIndent()

        val request = objectMapper.readValue<BrevbestillingRequest>(json)
        val utkast = request.tilUtkast()

        utkast.kontaktopplysninger shouldBe false
        utkast.skalViseStandardTekstOmOpplysninger shouldBe false
    }

    @Test
    fun `tilBrevbestillingDto konverterer null boolean til false`() {
        val json = """
            {
              "produserbardokument": null,
              "mottaker": null,
              "erInnvilgelse": null,
              "erEøsPensjonist": null,
              "erEøsLovvalg": null,
              "skalViseStandardTekstOmkontaktopplysninger": null,
              "skalViseStandardTekstOmOpplysninger": null
            }
        """.trimIndent()

        val request = objectMapper.readValue<BrevbestillingRequest>(json)
        val dto = request.tilBrevbestillingDto("testbruker")

        dto.isErInnvilgelse shouldBe false
        dto.isErEøsPensjonist shouldBe false
        dto.isErEøsLovvalg shouldBe false
        dto.isSkalViseStandardTekstOmKontaktopplysninger shouldBe false
        dto.isSkalViseStandardTekstOmOpplysninger shouldBe false
    }
}
