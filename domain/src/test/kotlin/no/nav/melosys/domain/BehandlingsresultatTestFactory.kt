package no.nav.melosys.domain

import no.nav.melosys.domain.BehandlingsresultatTestFactory.defaultBehandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.Årsavregning
import java.time.Instant

fun Behandlingsresultat.Companion.forTest(init: Behandlingsresultat.() -> Unit = {}): Behandlingsresultat =
    defaultBehandlingsresultat().apply(init)

fun Behandlingsresultat.behandling(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit) {
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

fun Behandlingsresultat.medlemskapsperiode(init: MedlemskapsperiodeTestFactory.Builder.() -> Unit) {
    val nyMedlemskapsperiode = medlemskapsperiodeForTest(init)
    addMedlemskapsperiode(nyMedlemskapsperiode)
}

fun Behandlingsresultat.lovvalgsperiode(init: LovvalgsperiodeTestFactory.Builder.() -> Unit) {
    val nyLovvalgsperiode = lovvalgsperiodeForTest(init).apply {
        behandlingsresultat = this@lovvalgsperiode
    }
    lovvalgsperioder.add(nyLovvalgsperiode)
}

fun Medlemskapsperiode.trygdeavgiftsperiode(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit) {
    val periode = Trygdeavgiftsperiode.forTest(init).apply {
        grunnlagMedlemskapsperiode = this@trygdeavgiftsperiode
    }
    trygdeavgiftsperioder.add(periode)
}

object BehandlingsresultatTestFactory {
    @JvmStatic
    fun defaultBehandlingsresultat() = Behandlingsresultat().apply {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}

