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
    class Builder {
        var periodeFra: LocalDate = PERIODE_FRA
        var periodeTil: LocalDate = PERIODE_TIL
        var trygdesats: BigDecimal = TRYGDESATS
        var trygdeavgiftsbeløpMd: BigDecimal = TRYGDEAVGIFTSBELØP_MD
        var medlemskapsperiode: Medlemskapsperiode? = null

        private var skatteforholdInit: (SkatteforholdTilNorge.() -> Unit)? = null
        private var inntektsperiodeInit: (Inntektsperiode.() -> Unit)? = null

        fun grunnlagSkatteforholdTilNorge(init: SkatteforholdTilNorge.() -> Unit) {
            skatteforholdInit = init
        }

        fun grunnlagInntekstperiode(init: Inntektsperiode.() -> Unit) {
            inntektsperiodeInit = init
        }

        fun build(): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            trygdesats = trygdesats,
            trygdeavgiftsbeløpMd = Penger(trygdeavgiftsbeløpMd),
            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                // Sett standardverdier fra periodedatoer
                this.fomDato = periodeFra
                this.tomDato = periodeTil
                // Sett skatteplikttype fra toppnivåegenskap (kan overstyres av nestet blokk)
                this.skatteplikttype = SKATTEPLIKTTYPE
                // Bruk tilpasset konfigurasjon hvis tilgjengelig (dette kan overstyre verdiene over)
                skatteforholdInit?.invoke(this)
            },
            grunnlagInntekstperiode = Inntektsperiode().apply {
                // Sett standardverdier fra periodedatoer
                this.fomDato = periodeFra
                this.tomDato = periodeTil
                // Sett standardverdier for påkrevde felt
                this.type = INNTEKTSKILDETYPE
                this.isArbeidsgiversavgiftBetalesTilSkatt = ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT
                this.avgiftspliktigMndInntekt = AVGIFTSPLIKTIG_MND_INNTEKT
                // Bruk tilpasset konfigurasjon hvis tilgjengelig (kan overstyre standardverdier)
                inntektsperiodeInit?.invoke(this)
            },
            grunnlagMedlemskapsperiode = medlemskapsperiode
        )
    }
}
