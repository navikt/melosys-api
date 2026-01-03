package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import java.time.LocalDate

fun anmodningEllerAttestForTest(init: AnmodningEllerAttestTestFactory.Builder.() -> Unit = {}): AnmodningEllerAttest =
    AnmodningEllerAttestTestFactory.Builder().apply(init).build()

object AnmodningEllerAttestTestFactory {
    val AVSENDERLAND = Land_iso2.SE
    val LOVVALGSLAND = Land_iso2.NO

    @MelosysTestDsl
    class Builder {
        var avsenderland: Land_iso2? = AVSENDERLAND
        var lovvalgsland: Land_iso2? = LOVVALGSLAND

        // Periode support (inherited from MottatteOpplysningerData)
        private var periodeFom: LocalDate? = null
        private var periodeTom: LocalDate? = null

        // Soeknadsland support (inherited from MottatteOpplysningerData)
        private var soeknadslandkoder: List<String> = emptyList()
        private var flereLandUkjentHvilke: Boolean = false

        /** Set periode with fom and tom dates */
        fun periode(fom: LocalDate, tom: LocalDate? = null) = apply {
            periodeFom = fom
            periodeTom = tom
        }

        /** Set søknadsland med landkoder */
        fun soeknadsland(vararg landkoder: String, flereLandUkjent: Boolean = false) = apply {
            soeknadslandkoder = landkoder.toList()
            flereLandUkjentHvilke = flereLandUkjent
        }

        /** Set søknadsland med Land_iso2 enum */
        fun soeknadsland(vararg land: Land_iso2, flereLandUkjent: Boolean = false) = apply {
            soeknadslandkoder = land.map { it.kode }
            flereLandUkjentHvilke = flereLandUkjent
        }

        fun build(): AnmodningEllerAttest = AnmodningEllerAttest().apply {
            this.avsenderland = this@Builder.avsenderland
            this.lovvalgsland = this@Builder.lovvalgsland

            // Set periode if fom is provided
            if (this@Builder.periodeFom != null) {
                this.periode = Periode(this@Builder.periodeFom, this@Builder.periodeTom)
            }

            // Set soeknadsland if any landkoder were provided
            if (this@Builder.soeknadslandkoder.isNotEmpty() || this@Builder.flereLandUkjentHvilke) {
                this.soeknadsland = Soeknadsland(
                    this@Builder.soeknadslandkoder,
                    this@Builder.flereLandUkjentHvilke
                )
            }
        }
    }
}
