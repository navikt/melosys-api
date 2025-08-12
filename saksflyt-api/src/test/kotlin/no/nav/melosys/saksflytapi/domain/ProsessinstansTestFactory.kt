package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.forTest
import java.util.*

fun prosessinstansForTest(init: Prosessinstans.Builder.() -> Unit = {}): Prosessinstans =
    ProsessinstansTestFactory.builderWithDefaults().apply(init).build()

fun Prosessinstans.Builder.behandling(init: Behandling.Builder.() -> Unit) = apply {
    this.behandling = Behandling.forTest(init)
}

object ProsessinstansTestFactory {
    @JvmStatic
    fun builderWithDefaults() = Prosessinstans.builder().apply {
        id = UUID.randomUUID()
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
    }
}
