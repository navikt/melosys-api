package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigDecimal
import java.time.LocalDate

fun trygdeavgiftsperiodeForTest(init: TrygdeavgiftsperiodeBuilder.() -> Unit = {}): Trygdeavgiftsperiode =
    TrygdeavgiftsperiodeBuilder().apply(init).build()

@MelosysTestDsl
class TrygdeavgiftsperiodeBuilder {
    var periodeFra: LocalDate = LocalDate.of(2023, 1, 1)
    var periodeTil: LocalDate = LocalDate.of(2023, 12, 31)
    var skatteplikttype: Skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    var trygdesats: BigDecimal = 6.8.toBigDecimal()
    var avgiftspliktigMndInntekt: BigDecimal = 10000.toBigDecimal()
    var trygdeavgiftsbeløpMd: BigDecimal = 1000.toBigDecimal()
    var medlemskapsperiode: Medlemskapsperiode? = null

    fun build(): Trygdeavgiftsperiode {
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            fomDato = this@TrygdeavgiftsperiodeBuilder.periodeFra
            tomDato = this@TrygdeavgiftsperiodeBuilder.periodeTil
            this.skatteplikttype = this@TrygdeavgiftsperiodeBuilder.skatteplikttype
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = this@TrygdeavgiftsperiodeBuilder.periodeFra
            tomDato = this@TrygdeavgiftsperiodeBuilder.periodeTil
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(this@TrygdeavgiftsperiodeBuilder.avgiftspliktigMndInntekt)
        }

        return Trygdeavgiftsperiode(
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            trygdesats = trygdesats,
            trygdeavgiftsbeløpMd = Penger(trygdeavgiftsbeløpMd),
            grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagMedlemskapsperiode = medlemskapsperiode
        )
    }
}
