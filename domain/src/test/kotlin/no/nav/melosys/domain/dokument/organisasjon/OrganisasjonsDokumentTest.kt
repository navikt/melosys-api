package no.nav.melosys.domain.dokument.organisasjon

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsDokumentTest {

    @Test
    fun `harRegistrertPostadresse skal bli false`() {

        val dokument = OrganisasjonDokumentTestFactory.builder().build().apply {
            organisasjonDetaljer.run {
                postadresse = listOf(lagAddresse().apply {
                    landkode = null
                })
            }
        }


        val harRegistrertPostadresse = dokument.harRegistrertPostadresse()


        harRegistrertPostadresse.shouldBeFalse()
    }

    @Test
    fun `harRegistrertPostadresse skal bli true`() {
        val dokument = OrganisasjonDokumentTestFactory.builder().build().apply {
            organisasjonDetaljer.run {
                postadresse = listOf(lagAddresse().apply {
                    landkode = "NO"
                })
            }
        }

        val harRegistrertPostadresse = dokument.harRegistrertPostadresse()


        harRegistrertPostadresse.shouldBeTrue()
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
