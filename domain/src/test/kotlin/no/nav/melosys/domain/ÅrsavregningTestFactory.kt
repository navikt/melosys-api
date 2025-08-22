package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.ÅrsavregningTestFactory.defaultÅrsavregning

fun Årsavregning.Companion.forTest(init: Årsavregning.() -> Unit = {}): Årsavregning =
    defaultÅrsavregning().apply(init)

object ÅrsavregningTestFactory {
    @JvmStatic
    fun defaultÅrsavregning() = Årsavregning(aar = 2000)

}
