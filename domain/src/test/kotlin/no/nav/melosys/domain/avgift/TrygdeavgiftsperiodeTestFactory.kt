package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigDecimal
import java.time.LocalDate

fun Trygdeavgiftsperiode.Companion.forTest(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit = {}): Trygdeavgiftsperiode =
    TrygdeavgiftsperiodeTestFactory.Builder().apply(init).build()

object TrygdeavgiftsperiodeTestFactory {
    val PERIODE_FRA = LocalDate.of(2023, 1, 1)
    val PERIODE_TIL = LocalDate.of(2023, 12, 31)
    val TRYGDESATS = BigDecimal(6.8)
    val TRYGDEAVGIFTSBELØP_MD = BigDecimal(1000)
    val SKATTEPLIKTTYPE = Skatteplikttype.IKKE_SKATTEPLIKTIG
    val INNTEKTSKILDETYPE = Inntektskildetype.INNTEKT_FRA_UTLANDET
    val AVGIFTSPLIKTIG_MND_INNTEKT = Penger(15000.0)
    const val ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT = false

    @MelosysTestDsl
    class InntektsperiodeBuilder {
        var fomDato: LocalDate? = null
        var tomDato: LocalDate? = null
        var type: Inntektskildetype? = null
        var avgiftspliktigMndInntekt: Penger? = null
        var arbeidsgiversavgiftBetalesTilSkatt: Boolean? = null

        fun build(defaultFom: LocalDate, defaultTom: LocalDate): Inntektsperiode = Inntektsperiode().apply {
            this.fomDato = this@InntektsperiodeBuilder.fomDato ?: defaultFom
            this.tomDato = this@InntektsperiodeBuilder.tomDato ?: defaultTom
            this.type = this@InntektsperiodeBuilder.type ?: INNTEKTSKILDETYPE
            this.isArbeidsgiversavgiftBetalesTilSkatt = this@InntektsperiodeBuilder.arbeidsgiversavgiftBetalesTilSkatt ?: ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT
            this.avgiftspliktigMndInntekt = this@InntektsperiodeBuilder.avgiftspliktigMndInntekt ?: AVGIFTSPLIKTIG_MND_INNTEKT
        }
    }

    @MelosysTestDsl
    class SkatteforholdTilNorgeBuilder {
        var fomDato: LocalDate? = null
        var tomDato: LocalDate? = null
        var skatteplikttype: Skatteplikttype? = null

        fun build(defaultFom: LocalDate, defaultTom: LocalDate): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
            this.fomDato = this@SkatteforholdTilNorgeBuilder.fomDato ?: defaultFom
            this.tomDato = this@SkatteforholdTilNorgeBuilder.tomDato ?: defaultTom
            this.skatteplikttype = this@SkatteforholdTilNorgeBuilder.skatteplikttype ?: SKATTEPLIKTTYPE
        }
    }

    @MelosysTestDsl
    class Builder {
        var periodeFra: LocalDate = PERIODE_FRA
        var periodeTil: LocalDate = PERIODE_TIL
        var trygdesats: BigDecimal = TRYGDESATS
        var trygdeavgiftsbeløpMd: BigDecimal = TRYGDEAVGIFTSBELØP_MD
        var medlemskapsperiode: Medlemskapsperiode? = null

        private val skatteforholdBuilder = SkatteforholdTilNorgeBuilder()
        private val inntektsperiodeBuilder = InntektsperiodeBuilder()

        fun grunnlagSkatteforholdTilNorge(init: SkatteforholdTilNorgeBuilder.() -> Unit) {
            skatteforholdBuilder.apply(init)
        }

        fun grunnlagInntekstperiode(init: InntektsperiodeBuilder.() -> Unit) {
            inntektsperiodeBuilder.apply(init)
        }

        fun build(): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            trygdesats = trygdesats,
            trygdeavgiftsbeløpMd = Penger(trygdeavgiftsbeløpMd),
            grunnlagSkatteforholdTilNorge = skatteforholdBuilder.build(periodeFra, periodeTil),
            grunnlagInntekstperiode = inntektsperiodeBuilder.build(periodeFra, periodeTil),
            grunnlagMedlemskapsperiode = medlemskapsperiode
        )
    }
}
