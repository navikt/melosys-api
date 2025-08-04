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
    fun representantIUtlandetMangler_ok_false() {
        ArbeidsstedRegler.representantIUtlandetMangler(lagRepresentantIUtlandet("RepresentantNavn")) shouldBe false
    }

    @Test
    fun representantIUtlandetMangler_finnesIkke_true() {
        ArbeidsstedRegler.representantIUtlandetMangler(null) shouldBe true
    }

    @Test
    fun representantIUtlandetMangler_harIkkeNavn_true() {
        ArbeidsstedRegler.representantIUtlandetMangler(lagRepresentantIUtlandet(null)) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_landErSJ_true() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("SJ", "by")) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_landErIkkeSJ_false() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("JS", "by")) shouldBe false
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_likByFraSvalbard_true() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Hopen")) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_ikkeÅlesundMenAlesund_true() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Ny-Alesund")) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_caseInsensitive_true() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", " NY-ÅLESUND ")) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_byIkkeFraSvalbard_false() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("JS", "New-Holesound")) shouldBe false
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_tekstInneholderSenjahopen_false() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Senjahopen")) shouldBe false
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_tekstInneholderByFraSvalbardIkkeHopen_true() {
        ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(lagSedDokument("NO", "Longyearbyen, Svalbard, Norway")) shouldBe true
    }

    @Test
    fun arbeidstedSvalbardOgJanMayen_tekstInneholderHopenMenIkkeHopen_false() {
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
