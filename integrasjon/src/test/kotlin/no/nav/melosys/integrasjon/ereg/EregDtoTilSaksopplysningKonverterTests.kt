package no.nav.melosys.integrasjon.ereg

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse.*
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime

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
        val organisasjon = Organisasjon(
            organisasjonsnummer = "928497705",
            navn = Navn(
                sammensattnavn = "Fra Organisasjon",
                bruksperiode = Bruksperiode(fom = LocalDateTime.now()),
                gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now()),
            ),
            organisasjonDetaljer = OrganisasjonDetaljer(
                navn = listOf(
                    Navn(
                        sammensattnavn = "Fra Detaljer",
                        bruksperiode = Bruksperiode(fom = LocalDateTime.now()),
                        gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now()),
                    )
                )
            )
        )


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .navn.shouldBe("Fra Organisasjon")
    }

    @Test
    fun `hent sammensatt navn i detaljer om ikke finnes i organisasjon`() {
        val organisasjon = Organisasjon(
            organisasjonsnummer = "928497705",
            organisasjonDetaljer = OrganisasjonDetaljer(
                navn = listOf(
                    Navn(
                        sammensattnavn = "Fra Detaljer",
                        bruksperiode = Bruksperiode(fom = LocalDateTime.now()),
                        gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now()),
                    )
                )
            )
        )


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .navn.shouldBe("Fra Detaljer")
    }

    @Test
    fun `hent navn i navnelinjer om ikke finnes som sammensatt navn i organisasjon eller detaljer`() {
        val organisasjon = Organisasjon(
            organisasjonsnummer = "928497705",
            organisasjonDetaljer = OrganisasjonDetaljer(
                navn = listOf(
                    Navn(
                        navnelinje1 = "navnelinje1",
                        navnelinje2 = "navnelinje2",
                        bruksperiode = Bruksperiode(fom = LocalDateTime.now()),
                        gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now()),
                    )
                )
            )
        )


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .navn.shouldBe("navnelinje1 navnelinje2")
    }

    @Test
    fun `mangler navn`() {
        val organisasjon = Organisasjon(
            organisasjonsnummer = "928497705",
            organisasjonDetaljer = OrganisasjonDetaljer()
        )


        val saksopplysning = EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjon)


        saksopplysning.dokument.shouldBeTypeOf<OrganisasjonDokument>()
            .navn.shouldBe("UKJENT")
    }

    private fun hentOrganisasjon(file: String) = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .readValue<Organisasjon>(hentRessurs("mock/organisasjon/konverter/$file"))

    private fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
