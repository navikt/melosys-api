package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.Land_iso2

fun anmodningEllerAttestForTest(init: AnmodningEllerAttestTestFactory.Builder.() -> Unit = {}): AnmodningEllerAttest =
    AnmodningEllerAttestTestFactory.Builder().apply(init).build()

object AnmodningEllerAttestTestFactory {
    val AVSENDERLAND = Land_iso2.SE
    val LOVVALGSLAND = Land_iso2.NO

    @MelosysTestDsl
    class Builder {
        var avsenderland: Land_iso2? = AVSENDERLAND
        var lovvalgsland: Land_iso2? = LOVVALGSLAND

        fun build(): AnmodningEllerAttest = AnmodningEllerAttest().apply {
            this.avsenderland = this@Builder.avsenderland
            this.lovvalgsland = this@Builder.lovvalgsland
        }
    }
}
