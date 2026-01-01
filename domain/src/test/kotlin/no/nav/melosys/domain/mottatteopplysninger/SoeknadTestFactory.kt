package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import java.time.LocalDate

fun soeknadForTest(init: SoeknadTestFactory.Builder.() -> Unit = {}): Soeknad =
    SoeknadTestFactory.Builder().apply(init).build()

object SoeknadTestFactory {
    @MelosysTestDsl
    class Builder {
        private val soeknadslandkoder = mutableListOf<String>()
        private val fysiskeArbeidssteder = mutableListOf<FysiskArbeidssted>()
        private val foretakUtlandListe = mutableListOf<ForetakUtland>()
        private val selvstendigForetakListe = mutableListOf<SelvstendigForetak>()
        private val ekstraArbeidsgivereListe = mutableListOf<String>()

        // Bosted-adressefelt
        var bostedLandkode: String? = null
        var bostedPoststed: String? = null
        var bostedGatenavn: String? = null
        var bostedHusnummer: String? = null
        var bostedPostnummer: String? = null

        var periodeFom: LocalDate? = null
        var periodeTom: LocalDate? = null

        // Direkte bosted-overstyring (for edge case-tester)
        private var customBosted: Bosted? = null

        /** Erstatt bosted helt (for edge case-tester som tomt bosted) */
        fun bosted(bosted: Bosted) = apply {
            customBosted = bosted
        }

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

        /** Legg til utenlandsk foretak */
        fun foretakUtland(orgnr: String, selvstendig: Boolean = false) = apply {
            foretakUtlandListe.add(ForetakUtland().apply {
                this.orgnr = orgnr
                this.selvstendigNæringsvirksomhet = selvstendig
            })
        }

        /** Legg til selvstendig foretak */
        fun selvstendigForetak(orgnr: String) = apply {
            selvstendigForetakListe.add(SelvstendigForetak().apply { this.orgnr = orgnr })
        }

        /** Legg til ekstra norsk arbeidsgiver (orgnr) */
        fun ekstraArbeidsgiver(orgnr: String) = apply {
            ekstraArbeidsgivereListe.add(orgnr)
        }

        /** Konfigurer bostedadresse med alle felt */
        fun bostedAdresse(
            landkode: String = "NO",
            poststed: String? = null,
            gatenavn: String? = null,
            husnummer: String? = null,
            postnummer: String? = null
        ) = apply {
            bostedLandkode = landkode
            bostedPoststed = poststed
            bostedGatenavn = gatenavn
            bostedHusnummer = husnummer
            bostedPostnummer = postnummer
        }

        fun build(): Soeknad = Soeknad().apply {
            // Sett søknadsland
            soeknadslandkoder.forEach { this.soeknadsland.landkoder.add(it) }

            // Sett arbeidssteder
            this.arbeidPaaLand.fysiskeArbeidssteder = this@Builder.fysiskeArbeidssteder

            // Sett bosted - enten egendefinert eller fra individuelle felt
            if (this@Builder.customBosted != null) {
                this.bosted = this@Builder.customBosted!!
            } else {
                this@Builder.bostedLandkode?.let { this.bosted.oppgittAdresse.landkode = it }
                this@Builder.bostedPoststed?.let { this.bosted.oppgittAdresse.poststed = it }
                this@Builder.bostedGatenavn?.let { this.bosted.oppgittAdresse.gatenavn = it }
                this@Builder.bostedHusnummer?.let { this.bosted.oppgittAdresse.husnummerEtasjeLeilighet = it }
                this@Builder.bostedPostnummer?.let { this.bosted.oppgittAdresse.postnummer = it }
            }

            // Sett periode
            if (this@Builder.periodeFom != null || this@Builder.periodeTom != null) {
                this.periode = Periode(this@Builder.periodeFom, this@Builder.periodeTom)
            }

            // Sett foretak utland
            this.foretakUtland.addAll(foretakUtlandListe)

            // Sett selvstendig foretak
            this.selvstendigArbeid.selvstendigForetak = selvstendigForetakListe

            // Sett ekstra arbeidsgivere
            this.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivereListe
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
