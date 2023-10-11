package no.nav.melosys.service.avgift

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

internal class TrygdeavgtiftsMottakerServiceTest {

    val trygdeavgiftsMottakerService = TrygdeavgiftsMottakerService()

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker ikke er skattepliktig og aga er false`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE))
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis bruker er skattepliktig og aga er true`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE))
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis bruker er skattepliktig og aga er false, men type er MISJONÆR`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode( false, MISJONÆR))
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker er ikke skattepliktig og aga er false, men type er MISJONÆR`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode( false, MISJONÆR))
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v1`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE))
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v2`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE)) }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v3`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(
                lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE),
                lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE)
            )
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v4`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(
                lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE),
                lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE)
            )
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v5`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(
                lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE),
                lagInntektsperiode( false, ARBEIDSINNTEKT_FRA_NORGE)
            )
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis det er flere innteksperioder med forskjellige mottakere`() {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
            skatteforholdTilNorge = mutableListOf(lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG))
            inntektsperioder = mutableListOf(
                lagInntektsperiode( true, ARBEIDSINNTEKT_FRA_NORGE),
                lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
            )
        }
        val result = trygdeavgiftsMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
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
