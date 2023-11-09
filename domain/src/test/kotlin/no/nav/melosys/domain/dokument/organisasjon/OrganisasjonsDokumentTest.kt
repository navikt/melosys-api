package no.nav.melosys.domain.dokument.organisasjon

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsDokumentTest {

    @Test
    fun `harRegistrertPostadresse skal bli false`() {
        val adresser = listOf(lagAddresse().apply {
            landkode = null
        })

        val dokument = OrganisasjonDokument().apply {
            organisasjonDetaljer = OrganisasjonsDetaljer().apply {
                postadresse = adresser

            }
        }

        dokument.harRegistrertPostadresse().shouldBeFalse()
    }

    @Test
    fun `harRegistrertPostadresse skal bli true`() {
        val adresser = listOf(lagAddresse().apply {
            landkode = "NO"
        })

        val dokument = OrganisasjonDokument().apply {
            organisasjonDetaljer = OrganisasjonsDetaljer().apply {
                postadresse = adresser

            }
        }

        dokument.harRegistrertPostadresse().shouldBeTrue()
    }

    @Test
    fun `lagSammenslåttNavn skal slå sammen navn`() {
        val dokument = OrganisasjonDokument().apply {
            navn = listOf("fornavn", "etternavn")
        }

        dokument.lagSammenslåttNavn().shouldBe("fornavn etternavn")
    }

    @Test
    fun `lagSammenslåttNavn skal bli UKJENT når navn = null`() {
        val dokument = OrganisasjonDokument().apply {
            navn = null
        }

        dokument.lagSammenslåttNavn().shouldBe("UKJENT")
    }

    @Test
    fun `lagSammenslåttNavn skal bli tom streng når navn er tom liste`() {
        val dokument = OrganisasjonDokument().apply {
            navn = emptyList()
        }

        dokument.lagSammenslåttNavn().shouldBe("") // Er dette ønsket?
    }

    private fun lagAddresse() = SemistrukturertAdresse().apply {
        postnr = "postnummer"
        poststed = "poststed"
        poststedUtland = "poststedUtland"
        kommunenr = "kommunenr"
        gyldighetsperiode = Periode(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1)
        )
    }
}
