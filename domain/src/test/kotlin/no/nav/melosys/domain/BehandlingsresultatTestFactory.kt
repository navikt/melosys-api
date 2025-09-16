package no.nav.melosys.domain

import no.nav.melosys.domain.BehandlingsresultatTestFactory.defaultBehandlingsresultat
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

fun Behandlingsresultat.medlemskapsperiode(init: Medlemskapsperiode.() -> Unit) {
    medlemskapsperioder.add(Medlemskapsperiode().apply(init))
}

object BehandlingsresultatTestFactory {
    @JvmStatic
    fun defaultBehandlingsresultat() = Behandlingsresultat().apply {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}

