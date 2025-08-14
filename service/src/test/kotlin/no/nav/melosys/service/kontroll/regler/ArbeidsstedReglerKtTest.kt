package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.melding.Adresse
import no.nav.melosys.domain.eessi.melding.Arbeidsland
import no.nav.melosys.domain.eessi.melding.Arbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet
import org.junit.jupiter.api.Test

class ArbeidsstedReglerKtTest {

    @Test
    fun `representantIUtlandetMangler skal returnere false når representant har navn`() {
        ArbeidsstedRegler.representantIUtlandetMangler(lagRepresentantIUtlandet("RepresentantNavn")) shouldBe false
    }

    @Test
    fun `representantIUtlandetMangler skal returnere true når representant er null`() {
        ArbeidsstedRegler.representantIUtlandetMangler(null) shouldBe true
    }

    @Test
    fun `representantIUtlandetMangler skal returnere true når representant ikke har navn`() {
        ArbeidsstedRegler.representantIUtlandetMangler(lagRepresentantIUtlandet(null)) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere true når land er SJ`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("SJ", "by")) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere false når land ikke er SJ`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("JS", "by")) shouldBe false
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere true for by fra Svalbard`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Hopen")) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere true for Ny-Alesund`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Ny-Alesund")) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal være case insensitive`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", " NY-ÅLESUND ")) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere false for by ikke fra Svalbard`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("JS", "New-Holesound")) shouldBe false
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere false for Senjahopen`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Senjahopen")) shouldBe false
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere true for Longyearbyen`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Longyearbyen, Svalbard, Norway")) shouldBe true
    }

    @Test
    fun `erArbeidslandFraSvalbardOgJanMayen skal returnere false når tekst inneholder Hopen men ikke er Hopen`() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Hopener Mühlenbach, Germany")) shouldBe false
    }

    private fun lagRepresentantIUtlandet(navn: String?) = RepresentantIUtlandet().apply {
        representantNavn = navn
    }

    private fun lagSedDokument(landKode: String, by: String): SedDokument {
        val arbeidssteder = listOf(
            Arbeidssted("sted1", Adresse().apply {
                this.by = "By_1"
                land = "XY"
            }), Arbeidssted("sted2", Adresse().apply {
                this.by = by
            })
        )

        return SedDokument().apply {
            arbeidsland = listOf(
                Arbeidsland(landKode, arbeidssteder),
                Arbeidsland(landKode, arbeidssteder)
            )
        }
    }
}
