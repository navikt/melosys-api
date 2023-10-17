package no.nav.melosys.integrasjon.ereg

import com.fasterxml.jackson.databind.DeserializationFeature
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




    fun hentOrganisasjon(file: String) = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/konverter/$file"))

    fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
