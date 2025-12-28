package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import java.time.LocalDate

fun soeknadForTest(init: SoeknadTestFactory.Builder.() -> Unit = {}): Soeknad =
    SoeknadTestFactory.Builder().apply(init).build()

object SoeknadTestFactory {
    @MelosysTestDsl
    class Builder {
        private val soeknadslandkoder = mutableListOf<String>()
        private val fysiskeArbeidssteder = mutableListOf<FysiskArbeidssted>()
        var bostedLandkode: String? = null
        var bostedPoststed: String? = null
        var periodeFom: LocalDate? = null
        var periodeTom: LocalDate? = null

        fun landkoder(vararg landkoder: String) = apply {
            soeknadslandkoder.addAll(landkoder)
        }

        fun fysiskeArbeidssted(init: FysiskArbeidsstedBuilder.() -> Unit) = apply {
            fysiskeArbeidssteder.add(FysiskArbeidsstedBuilder().apply(init).build())
        }

        fun periode(fom: LocalDate, tom: LocalDate) = apply {
            this.periodeFom = fom
            this.periodeTom = tom
        }

        fun build(): Soeknad = Soeknad().apply {
            // Sett søknadsland
            soeknadslandkoder.forEach { this.soeknadsland.landkoder.add(it) }

            // Sett arbeidssteder
            this.arbeidPaaLand.fysiskeArbeidssteder = this@Builder.fysiskeArbeidssteder

            // Sett bosted
            this@Builder.bostedLandkode?.let { this.bosted.oppgittAdresse.landkode = it }
            this@Builder.bostedPoststed?.let { this.bosted.oppgittAdresse.poststed = it }

            // Sett periode
            if (this@Builder.periodeFom != null || this@Builder.periodeTom != null) {
                this.periode = Periode(this@Builder.periodeFom, this@Builder.periodeTom)
            }
        }
    }

    class FysiskArbeidsstedBuilder {
        var virksomhetNavn: String? = null
        var landkode: String? = null
        var poststed: String? = null
        var gatenavn: String? = null
        var postnummer: String? = null

        fun build(): FysiskArbeidssted = FysiskArbeidssted().apply {
            this.virksomhetNavn = this@FysiskArbeidsstedBuilder.virksomhetNavn
            this@FysiskArbeidsstedBuilder.landkode?.let { this.adresse.landkode = it }
            this@FysiskArbeidsstedBuilder.poststed?.let { this.adresse.poststed = it }
            this@FysiskArbeidsstedBuilder.gatenavn?.let { this.adresse.gatenavn = it }
            this@FysiskArbeidsstedBuilder.postnummer?.let { this.adresse.postnummer = it }
        }
    }
}
