package no.nav.melosys.domain.folketrygden

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.MISJONÆR
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker.*
import org.junit.jupiter.api.Test

internal class FastsattTrygdeavgiftTest {

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis ordinær trygdeavgift og aga er false`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(lagInntektsperiode(false, false))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis ordinær trygdeavgift og aga er true`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(lagInntektsperiode(true, true))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis ordinær trygdeavgift er true og aga er false, men type er MISJONÆR`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(lagInntektsperiode(true, false, MISJONÆR))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v1`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(lagInntektsperiode(true, false))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v2`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(lagInntektsperiode(false, true))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v3`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(
                    lagInntektsperiode(false, true),
                    lagInntektsperiode(true, true)
                )
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis det er flere innteksperioder med forskjellige mottakere`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = mutableListOf(
                    lagInntektsperiode(true, true),
                    lagInntektsperiode(false, false)
                )
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    fun lagInntektsperiode(
        ordinærTrygdeavgiftBetalesTilSkatt: Boolean,
        arbeidsgiversavgiftBetalesTilSkatt: Boolean,
        inntektskildetype: Inntektskildetype? = null
    ): Inntektsperiode {
        val inntektsperiode = Inntektsperiode().apply {
            isOrdinærTrygdeavgiftBetalesTilSkatt = ordinærTrygdeavgiftBetalesTilSkatt
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt
            type = inntektskildetype
        }
        return inntektsperiode
    }
}
