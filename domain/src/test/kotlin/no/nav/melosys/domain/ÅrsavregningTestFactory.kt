package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import java.math.BigDecimal
import java.time.LocalDate

fun Årsavregning.Companion.forTest(init: ÅrsavregningTestFactory.Builder.() -> Unit = {}): Årsavregning =
    ÅrsavregningTestFactory.Builder().apply(init).build()

object ÅrsavregningTestFactory {
    val DEFAULT_ÅR = LocalDate.now().year

    @MelosysTestDsl
    class Builder {
        var id: Long? = 0
        var behandlingsresultat: Behandlingsresultat? = null
        var aar: Int = DEFAULT_ÅR
        var tidligereBehandlingsresultat: Behandlingsresultat? = null
        var tidligereFakturertBeloep: BigDecimal? = null
        var beregnetAvgiftBelop: BigDecimal? = null
        var tilFaktureringBeloep: BigDecimal? = null
        var harTrygdeavgiftFraAvgiftssystemet: Boolean? = null
        var trygdeavgiftFraAvgiftssystemet: BigDecimal? = null
        var manueltAvgiftBeloep: BigDecimal? = null
        var endeligAvgiftValg: EndeligAvgiftValg? = null
        var harSkjoennsfastsattInntektsgrunnlag: Boolean = false

        fun build(): Årsavregning = Årsavregning(
            id = id ?: error("id er påkrevd for Årsavregning"),
            behandlingsresultat = behandlingsresultat,
            aar = aar,
            tidligereBehandlingsresultat = tidligereBehandlingsresultat,
            tidligereFakturertBeloep = tidligereFakturertBeloep,
            beregnetAvgiftBelop = beregnetAvgiftBelop,
            tilFaktureringBeloep = tilFaktureringBeloep,
            harTrygdeavgiftFraAvgiftssystemet = harTrygdeavgiftFraAvgiftssystemet,
            trygdeavgiftFraAvgiftssystemet = trygdeavgiftFraAvgiftssystemet,
            manueltAvgiftBeloep = manueltAvgiftBeloep,
            endeligAvgiftValg = endeligAvgiftValg,
            harSkjoennsfastsattInntektsgrunnlag = harSkjoennsfastsattInntektsgrunnlag
        )
    }
}
