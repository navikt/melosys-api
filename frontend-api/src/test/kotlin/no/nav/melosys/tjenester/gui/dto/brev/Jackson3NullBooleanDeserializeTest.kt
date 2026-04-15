package no.nav.melosys.tjenester.gui.dto.brev

import io.kotest.matchers.shouldBe
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

/**
 * Verifiserer at DTOer med boolean-felt håndterer null korrekt etter Jackson 2→3 migreringen.
 * Jackson 3 har FAIL_ON_NULL_FOR_PRIMITIVES=true som standard.
 *
 * Alle DTOer som deserialiseres fra JSON (@RequestBody) og har boolean-felt må bruke
 * boxed Boolean for å akseptere null.
 */
class Jackson3NullBooleanDeserializeTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `FerdigbehandlingKontrollerDto - null boolean deserialiseres uten feil`() {
        val json = """
            {
              "behandlingID": 123,
              "vedtakstype": null,
              "behandlingsresultattype": null,
              "kontrollerSomSkalIgnoreres": null,
              "skalRegisteropplysningerOppdateres": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<FerdigbehandlingKontrollerDto>(json)

        dto.skalRegisteropplysningerOppdateres() shouldBe null
    }

    @Test
    fun `FerdigbehandlingKontrollerDto - manglende boolean deserialiseres uten feil`() {
        val json = """
            {
              "behandlingID": 456,
              "vedtakstype": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<FerdigbehandlingKontrollerDto>(json)

        dto.skalRegisteropplysningerOppdateres() shouldBe null
    }

    @Test
    fun `FerdigbehandlingKontrollerDto - eksplisitt boolean deserialiseres korrekt`() {
        val json = """
            {
              "behandlingID": 789,
              "vedtakstype": null,
              "skalRegisteropplysningerOppdateres": true
            }
        """.trimIndent()

        val dto = objectMapper.readValue<FerdigbehandlingKontrollerDto>(json)

        dto.skalRegisteropplysningerOppdateres() shouldBe true
    }

    @Test
    fun `VilkaarDto - null oppfylt deserialiseres uten feil`() {
        val json = """
            {
              "vilkaar": "VESENTLIG_VIRKSOMHET",
              "oppfylt": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<VilkaarDto>(json)

        dto.isOppfylt shouldBe false
    }

    @Test
    fun `VilkaarDto - manglende oppfylt defaulter til false`() {
        val json = """
            {
              "vilkaar": "VESENTLIG_VIRKSOMHET"
            }
        """.trimIndent()

        val dto = objectMapper.readValue<VilkaarDto>(json)

        dto.isOppfylt shouldBe false
    }

    @Test
    fun `JournalfoeringTilordneDto - null boolean-felt deserialiseres uten feil`() {
        val json = """
            {
              "journalpostID": "123",
              "skalTilordnes": null,
              "ingenVurdering": null
            }
        """.trimIndent()

        val dto = objectMapper.readValue<JournalfoeringTilordneDto>(json)

        dto.isSkalTilordnes shouldBe false
        dto.isIngenVurdering shouldBe false
    }

    @Test
    fun `JournalfoeringTilordneDto - eksplisitte boolean-verdier fungerer`() {
        val json = """
            {
              "journalpostID": "456",
              "skalTilordnes": true,
              "ingenVurdering": true
            }
        """.trimIndent()

        val dto = objectMapper.readValue<JournalfoeringTilordneDto>(json)

        dto.isSkalTilordnes shouldBe true
        dto.isIngenVurdering shouldBe true
    }
}
