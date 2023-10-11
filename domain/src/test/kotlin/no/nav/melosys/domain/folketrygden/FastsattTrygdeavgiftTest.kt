package no.nav.melosys.domain.folketrygden

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.MISJONÆR
import no.nav.melosys.domain.kodeverk.Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
import no.nav.melosys.domain.kodeverk.Skatteplikttype

import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker.*
import org.junit.jupiter.api.Test

internal class FastsattTrygdeavgiftTest {

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis ordinær trygdeavgift og aga er false`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis SKATTEPLIKTIG og aga er true`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis SKATTEPLIKTIG og aga er false, men type er MISJONÆR`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( false, MISJONÆR))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis IKKE_SKATTEPLIKTIG og aga er false, men type er MISJONÆR`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( false, MISJONÆR))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v1`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v2`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE))
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis ordinær trygdeavgift og aga er en kombinasjon v3`() {
        FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                    lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(
                    lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE),
                    lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE)
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
                skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                    lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
                inntektsperioder = mutableListOf(
                    lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE),
                    lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                )
            }
        }.run {
            trygdeavgiftMottaker.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        }
    }

    fun lagInntektsperiode(
        arbeidsgiversavgiftBetalesTilSkatt: Boolean,
        inntektskildetype: Inntektskildetype
    ): Inntektsperiode {
        val inntektsperiode = Inntektsperiode().apply {
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt
            type = inntektskildetype
        }
        return inntektsperiode
    }

    fun lagSkatteforholdsperiode(
        skatteplikttype: Skatteplikttype
    ): SkatteforholdTilNorge {
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            setSkatteplikttype(skatteplikttype)
        }
        return skatteforholdTilNorge
    }
}
