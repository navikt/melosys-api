package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
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
        private val oppholdUtlandLandkoder = mutableListOf<String>()
        private val maritimtArbeidListe = mutableListOf<MaritimtArbeid>()
        private val utenlandskIdentListe = mutableListOf<UtenlandskIdent>()

        /** Selvstendig næringsvirksomhet flag */
        var erSelvstendig: Boolean = false

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

        /** Legg til et eksisterende ForetakUtland-objekt direkte */
        fun foretakUtland(foretak: ForetakUtland) = apply {
            foretakUtlandListe.add(foretak)
        }

        /** Legg til selvstendig foretak */
        fun selvstendigForetak(orgnr: String) = apply {
            selvstendigForetakListe.add(SelvstendigForetak().apply { this.orgnr = orgnr })
        }

        /** Legg til ekstra norsk arbeidsgiver (orgnr) */
        fun ekstraArbeidsgiver(orgnr: String) = apply {
            ekstraArbeidsgivereListe.add(orgnr)
        }

        /** Legg til oppholdsland i utlandet */
        fun oppholdUtland(vararg landkoder: String) = apply {
            oppholdUtlandLandkoder.addAll(landkoder)
        }

        /** Legg til maritimt arbeid */
        fun maritimtArbeid(init: MaritimtArbeidBuilder.() -> Unit) = apply {
            maritimtArbeidListe.add(MaritimtArbeidBuilder().apply(init).build())
        }

        /** Legg til utenlandsk ident */
        fun utenlandskIdent(ident: String, landkode: String) = apply {
            utenlandskIdentListe.add(UtenlandskIdent().apply {
                this.ident = ident
                this.landkode = landkode
            })
        }

        /** Legg til foretak utland med full konfigurasjon */
        fun foretakUtlandMedDetaljer(init: ForetakUtlandBuilder.() -> Unit) = apply {
            foretakUtlandListe.add(ForetakUtlandBuilder().apply(init).build())
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
            this.selvstendigArbeid.erSelvstendig = this@Builder.erSelvstendig

            // Sett utenlandsk ident
            if (utenlandskIdentListe.isNotEmpty()) {
                this.personOpplysninger.utenlandskIdent = utenlandskIdentListe
            }

            // Sett ekstra arbeidsgivere
            this.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivereListe

            // Sett opphold i utland
            if (oppholdUtlandLandkoder.isNotEmpty()) {
                this.oppholdUtland.oppholdslandkoder = oppholdUtlandLandkoder.toList()
            }

            // Sett maritimt arbeid
            this.maritimtArbeid = maritimtArbeidListe
        }
    }

    @MelosysTestDsl
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

    @MelosysTestDsl
    class MaritimtArbeidBuilder {
        var territorialfarvannLandkode: String? = null
        var flaggLandkode: String? = null
        var enhetNavn: String? = null

        fun build(): MaritimtArbeid = MaritimtArbeid().apply {
            this@MaritimtArbeidBuilder.territorialfarvannLandkode?.let { this.territorialfarvannLandkode = it }
            this@MaritimtArbeidBuilder.flaggLandkode?.let { this.flaggLandkode = it }
            this@MaritimtArbeidBuilder.enhetNavn?.let { this.enhetNavn = it }
        }
    }

    @MelosysTestDsl
    class ForetakUtlandBuilder {
        var orgnr: String? = null
        var navn: String? = null
        var uuid: String? = null
        var selvstendigNæringsvirksomhet: Boolean = false

        // Adressefelt
        private var adresse: StrukturertAdresse? = null

        fun adresse(
            landkode: String,
            gatenavn: String? = null,
            husnummer: String? = null,
            postnummer: String? = null,
            poststed: String? = null,
            region: String? = null
        ) = apply {
            adresse = StrukturertAdresse(
                husnummerEtasjeLeilighet = husnummer,
                gatenavn = gatenavn,
                postnummer = postnummer,
                poststed = poststed,
                region = region,
                landkode = landkode
            )
        }

        fun adresse(strukturertAdresse: StrukturertAdresse) = apply {
            adresse = strukturertAdresse
        }

        fun build(): ForetakUtland = ForetakUtland().apply {
            this@ForetakUtlandBuilder.orgnr?.let { this.orgnr = it }
            this@ForetakUtlandBuilder.navn?.let { this.navn = it }
            this@ForetakUtlandBuilder.uuid?.let { this.uuid = it }
            this.selvstendigNæringsvirksomhet = this@ForetakUtlandBuilder.selvstendigNæringsvirksomhet
            this@ForetakUtlandBuilder.adresse?.let { this.adresse = it }
        }
    }
}
