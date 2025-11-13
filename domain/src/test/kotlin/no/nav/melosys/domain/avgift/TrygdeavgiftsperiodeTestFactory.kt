package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigDecimal
import java.time.LocalDate

fun Trygdeavgiftsperiode.Companion.forTest(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit = {}): Trygdeavgiftsperiode =
    TrygdeavgiftsperiodeTestFactory.Builder().apply(init).build()

fun skatteforholdForTest(init: TrygdeavgiftsperiodeTestFactory.SkatteforholdTilNorgeBuilder.() -> Unit): SkatteforholdTilNorge =
    TrygdeavgiftsperiodeTestFactory.SkatteforholdTilNorgeBuilder().apply { init() }
        .build()

fun inntektForTest(init: TrygdeavgiftsperiodeTestFactory.InntektsperiodeBuilder.() -> Unit): Inntektsperiode =
    TrygdeavgiftsperiodeTestFactory.InntektsperiodeBuilder().apply { init() }
        .build()


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
        var id: Long? = null
        var fomDato: LocalDate? = null
        var tomDato: LocalDate? = null
        var type: Inntektskildetype? = INNTEKTSKILDETYPE
        var avgiftspliktigMndInntekt: Penger? = AVGIFTSPLIKTIG_MND_INNTEKT
        var avgiftspliktigTotalinntekt: Penger? = null
        var arbeidsgiversavgiftBetalesTilSkatt: Boolean = ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT

        fun build(): Inntektsperiode = Inntektsperiode().apply {
            this.id = this@InntektsperiodeBuilder.id
            this.fomDato = this@InntektsperiodeBuilder.fomDato
            this.tomDato = this@InntektsperiodeBuilder.tomDato
            this.type = this@InntektsperiodeBuilder.type
            this.isArbeidsgiversavgiftBetalesTilSkatt = this@InntektsperiodeBuilder.arbeidsgiversavgiftBetalesTilSkatt
            this.avgiftspliktigTotalinntekt = this@InntektsperiodeBuilder.avgiftspliktigTotalinntekt
            this.avgiftspliktigMndInntekt = this@InntektsperiodeBuilder.avgiftspliktigMndInntekt
        }
    }

    @MelosysTestDsl
    class SkatteforholdTilNorgeBuilder {
        var id: Long? = null
        var fomDato: LocalDate? = null
        var tomDato: LocalDate? = null
        var skatteplikttype: Skatteplikttype? = null

        fun build(): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
            this.id = this@SkatteforholdTilNorgeBuilder.id
            this.fomDato = this@SkatteforholdTilNorgeBuilder.fomDato
            this.tomDato = this@SkatteforholdTilNorgeBuilder.tomDato
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

        fun build(): Trygdeavgiftsperiode {
            skatteforholdBuilder.apply {
                fomDato = fomDato ?: this@Builder.periodeFra
                tomDato = tomDato ?: this@Builder.periodeTil
            }

            inntektsperiodeBuilder.apply {
                fomDato = fomDato ?: this@Builder.periodeFra
                tomDato = tomDato ?: this@Builder.periodeTil
            }

            return Trygdeavgiftsperiode(
                periodeFra = periodeFra,
                periodeTil = periodeTil,
                trygdesats = trygdesats,
                trygdeavgiftsbeløpMd = Penger(trygdeavgiftsbeløpMd),
                grunnlagSkatteforholdTilNorge = skatteforholdBuilder.build(),
                grunnlagInntekstperiode = inntektsperiodeBuilder.build(),
                grunnlagMedlemskapsperiode = medlemskapsperiode,
            )
        }
    }
}
