package no.nav.melosys.domain.dokument.organisasjon

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.OrganisasjonsDetaljerTestFactory
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsDetaljerTest {

    @Test
    fun `Konverter forretningsadresse til ustrukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagAdresse(landkode = "NO"))
            .build()


        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()


        resultatAdresse.shouldNotBeNull().run {
            getAdresselinje(1).shouldBe(LINJE1)
            getAdresselinje(2).shouldBe(LINJE2)
            getAdresselinje(3).shouldBe(LINJE3)
            getAdresselinje(4).shouldBe("$POSTNR $POSTSTED")
            landkode.shouldBe("NO")
        }
    }

    @Test
    fun `Konverter utenlandsk forretningsadresse til ustrukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagAdresse(landkode = "DK"))
            .build()


        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()


        resultatAdresse.shouldNotBeNull().run {
            getAdresselinje(1).shouldBe(LINJE1)
            getAdresselinje(2).shouldBe(LINJE2)
            getAdresselinje(3).shouldBe(LINJE3)
            getAdresselinje(4).shouldBe(POSTSTED_UTLAND)
            landkode.shouldBe("DK")
        }
    }

    @Test
    fun `Konverter forretningsadresse til strukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagAdresse(landkode = "NO"))
            .build()


        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()


        resultatAdresse.shouldNotBeNull().run {
            gatenavn.shouldBe(listOf(LINJE1.trim(), LINJE2, LINJE3).joinToString(" "))
            landkode.shouldBe("NO")
            postnummer.shouldBe(POSTNR)
            poststed.shouldBe(POSTSTED)
        }
    }

    @Test
    fun `Konverter utenlandsk forretningsadresse til strukturert adresse`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagAdresse(landkode = "DK"))
            .build()


        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()


        resultatAdresse.shouldNotBeNull().run {
            gatenavn.shouldBe(listOf(LINJE1.trim(), LINJE2, LINJE3).joinToString(" "))
            landkode.shouldBe("DK")
            postnummer.shouldBe(POSTNR)
            poststed.shouldBe(POSTSTED_UTLAND)
        }
    }

    @Test
    fun `Test null adresse`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder().build()


        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()


        resultatAdresse.shouldBeNull()
    }

    @Test
    fun `Konverter utenlandsk adresse med null postnummer til strukturert adresse postnummer som ett mellomrom`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder().build()
        val adresse = SemistrukturertAdresse().apply {
            landkode = "US"
            postnr = null
            poststedUtland = "New York"
        }


        val resultatAdresse = orgDetaljer.konverterTilStrukturertAdresse(adresse)


        resultatAdresse.shouldNotBeNull().run {
            landkode.shouldBe("US")
            postnummer.shouldBe(" ")
            poststed.shouldBe("New York")
        }
    }

    @Test
    fun `Konverter norsk adresse med null postnummer til strukturert adresse med postnummer=null`() {
        val orgDetaljer = OrganisasjonsDetaljerTestFactory.builder().build()
        val adresse = SemistrukturertAdresse().apply {
            landkode = "NO"
            postnr = null
            poststedUtland = null
        }


        val resultatAdresse = orgDetaljer.konverterTilStrukturertAdresse(adresse)


        resultatAdresse.shouldNotBeNull().run {
            landkode.shouldBe("NO")
            postnummer.shouldBe(null)
            poststed.shouldBe("")
        }
    }


    private fun lagAdresse(landkode: String = "") = SemistrukturertAdresse().apply {
        adresselinje1 = LINJE1
        adresselinje2 = LINJE2
        adresselinje3 = LINJE3
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

    companion object {
        private const val LINJE1 = "LINJE1  "
        private const val LINJE2 = "LINJE2"
        private const val LINJE3 = "LINJE3"
        private const val POSTNR = "postnummer"
        private const val POSTSTED = "poststed"
        private const val POSTSTED_UTLAND = "poststedUtland"

    }
}

