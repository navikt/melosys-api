package no.nav.melosys.integrasjon.ereg

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class EregDtoTilSaksopplysningKonverterTests {

    @Test
    fun `finn enhetstype fra type detaljer før enhetstyper brukes `() {
        val organisasjon = hentOrganisasjon("enhetstype-1.json")

        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)

        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .enhetstype.shouldBe("BEDR")
    }

    @Test
    fun `finn enhetstype fra enhetstyper og bruk den første i lista når vi ikke har type detaljer `() {
        val organisasjon = hentOrganisasjon("enhetstype-2.json")

        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)

        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .enhetstype.shouldBe("FØRSTE")
    }

    @Test
    fun `hent sammensatt navn i organisasjon`() {
        val organisasjon: OrganisasjonResponse.Organisasjon = jacksonObjectMapper().createObjectNode().apply {
            put("organisasjonsnummer", "928497705")
            put("type", "Organisasjon")
            putObject("navn").apply {
                put("sammensattnavn", "Fra Organisasjon")
                putObject("bruksperiode").put("fom", "2021-03-02T14:19:37.229")
                putObject("gyldighetsperiode").put("fom", "2021-03-02")
            }

            putObject("organisasjonDetaljer").apply {
                putArray("navn").addObject().apply {
                    put("sammensattnavn", "Fra Detaljer")
                    putObject("bruksperiode").put("fom", "2021-06-02T09:23:59")
                    putObject("gyldighetsperiode").put("fom", "2021-06-02")
                }
            }
        }.toOrganisasjon()


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .getSammenslåttNavn().shouldBe("Fra Organisasjon")
    }

    @Test
    fun `hent sammensatt navn i detaljer om ikke finnes i organisasjon`() {
        val organisasjon: OrganisasjonResponse.Organisasjon = jacksonObjectMapper().createObjectNode().apply {
            put("organisasjonsnummer", "928497705")
            put("type", "Organisasjon")

            putObject("organisasjonDetaljer").apply {
                putArray("navn").addObject().apply {
                    put("sammensattnavn", "Fra Detaljer")
                    putObject("bruksperiode").put("fom", "2021-06-02T09:23:59")
                    putObject("gyldighetsperiode").put("fom", "2021-06-02")
                }
            }
        }.toOrganisasjon()


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .getSammenslåttNavn().shouldBe("Fra Detaljer")
    }

    @Test
    fun `hent navn i navnelinjer om ikke finnes som sammensatt navn i organisasjon eller detaljer`() {
        val organisasjon: OrganisasjonResponse.Organisasjon = jacksonObjectMapper().createObjectNode().apply {
            put("organisasjonsnummer", "928497705")
            put("type", "Organisasjon")

            putObject("organisasjonDetaljer").apply {
                putArray("navn").addObject().apply {
                    put("navnelinje1", "navnelinje1")
                    put("navnelinje2", "navnelinje2")
                    putObject("bruksperiode").put("fom", "2021-06-02T09:23:59")
                    putObject("gyldighetsperiode").put("fom", "2021-06-02")
                }
            }
        }.toOrganisasjon()


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .getSammenslåttNavn().shouldBe("navnelinje1 navnelinje2")
    }

    @Test
    fun `mangler navn`() {
        val organisasjon: OrganisasjonResponse.Organisasjon = jacksonObjectMapper().createObjectNode().apply {
            put("organisasjonsnummer", "928497705")
            put("type", "Organisasjon")

            putObject("organisasjonDetaljer")
        }.toOrganisasjon()


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .getSammenslåttNavn().shouldBe("UKJENT")
    }

    private fun hentOrganisasjon(file: String) = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/konverter/$file"))

    private fun JsonNode.toOrganisasjon(): OrganisasjonResponse.Organisasjon = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .readValue(this.toString())


    private fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
