package no.nav.melosys.domain.dokument.organisasjon

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsDetaljerTest {

    private val linje1 = "LINJE1  "
    private val linje2 = "LINJE2"
    private val linje3 = "LINJE3"
    private val POSTNR = "postnummer"
    private val POSTSTED = "poststed"
    private val POSTSTED_UTLAND = "poststedUtland"

    @Test
    fun `Konverter forretningsadresse til ustrukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf(lagAdresse(landkode = "NO"))
        }


        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()!!


        resultatAdresse.run {
            getAdresselinje(1).shouldBe(linje1)
            getAdresselinje(2).shouldBe(linje2)
            getAdresselinje(3).shouldBe(linje3)
            getAdresselinje(4).shouldBe("$POSTNR $POSTSTED")
            landkode.shouldBe("NO")
        }
    }

    @Test
    fun `Konverter utenlandsk forretningsadresse til ustrukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf(lagAdresse(landkode = "DK"))
        }

        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()!!

        resultatAdresse.run {
            getAdresselinje(1).shouldBe(linje1)
            getAdresselinje(2).shouldBe(linje2)
            getAdresselinje(3).shouldBe(linje3)
            getAdresselinje(4).shouldBe(POSTSTED_UTLAND)
            landkode.shouldBe("DK")
        }
    }

    @Test
    fun `Konverter forretningsadresse til strukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf(lagAdresse(landkode = "NO"))
        }

        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()!!

        resultatAdresse.run {
            gatenavn.shouldBe(listOf(linje1.trim(), linje2, linje3).joinToString(" "))
            landkode.shouldBe("NO")
            postnummer.shouldBe(POSTNR)
            poststed.shouldBe(POSTSTED)
        }
    }

    @Test
    fun `Konverter utenlandsk forretningsadresse til strukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf(lagAdresse("DK"))
        }

        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()!!

        resultatAdresse.run {
            gatenavn.shouldBe(listOf(linje1.trim(), linje2, linje3).joinToString(" "))
            landkode.shouldBe("DK")
            postnummer.shouldBe(POSTNR)
            poststed.shouldBe(POSTSTED_UTLAND)
        }
    }

    @Test
    fun `Test null adresse`() {
        val orgDetaljer = OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf()
        }

        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()
        resultatAdresse.shouldBeNull()
    }

    private fun lagAdresse(landkode: String = "") = SemistrukturertAdresse().apply {
        adresselinje1 = linje1
        adresselinje2 = linje2
        adresselinje3 = linje3
        postnr = POSTNR
        poststed = POSTSTED
        poststedUtland = POSTSTED_UTLAND
        kommunenr = "kommunenr"
        gyldighetsperiode = Periode(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1)
        )
        this.landkode = landkode
    }
}

