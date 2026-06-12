package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.saksflyt.steg.fakturering.mapTilFakturaperioder
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FakturabeskrivelseMapperTest {

    @Test
    fun `ordinær periode inkluderer inntekt, dekning og sats i beskrivelse`() {
        val medlemskapsperiode = medlemskapsperiodeForTest {
            trygdeavgiftsperiode {
                beregningsregel = Avgiftsberegningsregel.ORDINÆR
                trygdesats = BigDecimal("6.8")
                grunnlagInntekstperiode { type = Inntektskildetype.PENSJON_UFØRETRYGD }
            }
        }
        val periode = medlemskapsperiode.trygdeavgiftsperioder.first()

        val resultat = mapTilFakturaperioder(listOf(periode))

        resultat[0].beskrivelse.shouldContain("Inntekt:")
        resultat[0].beskrivelse.shouldContain("Dekning: Helsedel")
        resultat[0].beskrivelse.shouldContain("Sats: 6.8 %")
    }

    @Test
    fun `25-prosentregel-periode har tom beskrivelse - ingen inntekt, dekning eller sats`() {
        val periode = Trygdeavgiftsperiode.forTest {
            beregningsregel = Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
            trygdesats = null
        }

        val resultat = mapTilFakturaperioder(listOf(periode))

        resultat[0].beskrivelse.shouldBeEmpty()
    }

    @Test
    fun `miks av ordinær og 25-prosentregel viser beskrivelse kun for ordinær`() {
        val medlemskapsperiode = medlemskapsperiodeForTest {
            trygdeavgiftsperiode {
                beregningsregel = Avgiftsberegningsregel.ORDINÆR
                trygdesats = BigDecimal("6.8")
                grunnlagInntekstperiode { type = Inntektskildetype.PENSJON_UFØRETRYGD }
            }
            trygdeavgiftsperiode {
                beregningsregel = Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
                trygdesats = null
                trygdeavgiftsbeløpMd = BigDecimal("500")
            }
        }
        val perioder = medlemskapsperiode.trygdeavgiftsperioder.toList()
        val ordinær = perioder.first { it.beregningsregel == Avgiftsberegningsregel.ORDINÆR }
        val tjuefemProsent = perioder.first { it.beregningsregel == Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL }

        val resultat = mapTilFakturaperioder(listOf(ordinær, tjuefemProsent))

        resultat[0].beskrivelse.shouldContain("Inntekt:")
        resultat[0].beskrivelse.shouldContain("Sats: 6.8 %")
        resultat[1].beskrivelse.shouldBeEmpty()
    }

    @Test
    fun `25-prosentregel med prefiks viser kun prefiks i beskrivelse`() {
        val periode = Trygdeavgiftsperiode.forTest {
            beregningsregel = Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
            trygdesats = null
        }

        val resultat = mapTilFakturaperioder(listOf(periode), prefiks = "Satsendring")

        resultat[0].beskrivelse shouldBe "Satsendring"
    }

    @Test
    fun `inkluderDekning=false utelater dekning for ordinær periode`() {
        val medlemskapsperiode = medlemskapsperiodeForTest {
            trygdeavgiftsperiode {
                beregningsregel = Avgiftsberegningsregel.ORDINÆR
                trygdesats = BigDecimal("6.8")
                grunnlagInntekstperiode { type = Inntektskildetype.PENSJON_UFØRETRYGD }
            }
        }
        val periode = medlemskapsperiode.trygdeavgiftsperioder.first()

        val resultat = mapTilFakturaperioder(listOf(periode), inkluderDekning = false)

        resultat[0].beskrivelse.shouldContain("Inntekt:")
        resultat[0].beskrivelse.shouldNotContain("Dekning:")
        resultat[0].beskrivelse.shouldContain("Sats: 6.8 %")
    }
}
