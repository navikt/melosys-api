package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import java.math.BigDecimal
import java.time.LocalDate

fun Årsavregning.Companion.forTest(init: ÅrsavregningTestFactory.Builder.() -> Unit = {}): Årsavregning =
    ÅrsavregningTestFactory.Builder().apply(init).build()

// Note: behandlingsresultat extension is defined in no.nav.melosys.domain.BehandlingsresultatTestFactory
// to avoid circular dependency and ambiguity issues

object ÅrsavregningTestFactory {
    val DEFAULT_AAR = LocalDate.now().year

    @MelosysTestDsl
    class Builder {
        var id: Long? = 0
        var behandlingsresultat: Behandlingsresultat? = null
        var aar: Int = DEFAULT_AAR
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
            id = id ?: error("id er paakrevd for Aarsavregning"),
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
