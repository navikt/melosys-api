package no.nav.melosys.domain

import no.nav.melosys.domain.BehandlingsresultatTestFactory.defaultBehandlingsresultat
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeBuilder
import no.nav.melosys.domain.avgift.trygdeavgiftsperiodeForTest
import no.nav.melosys.domain.avgift.Årsavregning
import java.time.Instant

fun behandlingsresultatForTest(init: Behandlingsresultat.() -> Unit = {}): Behandlingsresultat =
    defaultBehandlingsresultat().apply(init)

fun Behandlingsresultat.behandling(init: Behandling.Builder.() -> Unit) {
    behandling = BehandlingTestFactory.builderWithDefaults().apply(init).build()
}

fun Behandlingsresultat.vedtakMetadata(init: VedtakMetadata.() -> Unit) {
    vedtakMetadata = VedtakMetadata().apply {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }.apply(init).apply {
        behandlingsresultat = this@vedtakMetadata
    }
}

fun Behandlingsresultat.årsavregning(init: Årsavregning.() -> Unit) {
    årsavregning = Årsavregning.forTest(init).apply {
        behandlingsresultat = this@årsavregning
    }
}

fun Behandlingsresultat.medlemskapsperiode(init: Medlemskapsperiode.() -> Unit) {
    val nyMedlemskapsperiode = Medlemskapsperiode().apply(init)
    addMedlemskapsperiode(nyMedlemskapsperiode)

    trygdeavgiftsperioder.forEach {
        it.grunnlagMedlemskapsperiode = nyMedlemskapsperiode
    }
}

fun Medlemskapsperiode.trygdeavgiftsperiode(init: TrygdeavgiftsperiodeBuilder.() -> Unit) {
    val periode = trygdeavgiftsperiodeForTest(init).apply {
        grunnlagMedlemskapsperiode = this@trygdeavgiftsperiode
    }
    trygdeavgiftsperioder = setOf(periode)
}

object BehandlingsresultatTestFactory {
    @JvmStatic
    fun defaultBehandlingsresultat() = Behandlingsresultat().apply {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}

