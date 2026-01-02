package no.nav.melosys.domain.dokument

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.Familiemedlem
import no.nav.melosys.domain.dokument.person.Familierelasjon
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.Personstatus
import no.nav.melosys.domain.dokument.person.Sivilstand
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import java.time.LocalDate

fun personDokumentForTest(init: PersonDokumentTestFactory.Builder.() -> Unit = {}): PersonDokument =
    PersonDokumentTestFactory.Builder().apply(init).build()

object PersonDokumentTestFactory {
    const val FNR = "12345678901"
    const val FORNAVN = "Ola"
    const val ETTERNAVN = "Nordmann"

    @MelosysTestDsl
    class Builder {
        var fnr: String? = FNR
        var fornavn: String? = FORNAVN
        var etternavn: String? = ETTERNAVN
        var sammensattNavn: String? = null
        var fødselsdato: LocalDate? = LocalDate.of(1990, 1, 1)
        var statsborgerskap: Land? = Land("NOR")
        var kjønn: KjoennsType? = KjoennsType("M")
        var sivilstand: Sivilstand? = null
        var personstatus: Personstatus? = null
        var bostedsadresse: Bostedsadresse? = null
        var erEgenAnsatt: Boolean = false

        private val familiemedlemmerListe = mutableListOf<Familiemedlem>()

        /** Legg til et familiemedlem */
        fun familiemedlem(init: FamiliemedlemBuilder.() -> Unit) = apply {
            familiemedlemmerListe.add(FamiliemedlemBuilder().apply(init).build())
        }

        fun build(): PersonDokument =
            PersonDokument(
                fnr = fnr,
                statsborgerskap = statsborgerskap,
                kjønn = kjønn,
                fødselsdato = fødselsdato,
                sivilstand = sivilstand,
                personstatus = personstatus,
                bostedsadresse = bostedsadresse
            ).apply {
                this.fornavn = this@Builder.fornavn
                this.etternavn = this@Builder.etternavn
                this.sammensattNavn = this@Builder.sammensattNavn
                this.setErEgenAnsatt(this@Builder.erEgenAnsatt)
                if (this@Builder.familiemedlemmerListe.isNotEmpty()) {
                    this.familiemedlemmer = this@Builder.familiemedlemmerListe
                }
            }
    }

    @MelosysTestDsl
    class FamiliemedlemBuilder {
        var fnr: String? = null
        var navn: String? = null
        var familierelasjon: Familierelasjon? = null
        var fødselsdato: LocalDate? = null
        var borMedBruker: Boolean = false

        fun build(): Familiemedlem = Familiemedlem().apply {
            this.fnr = this@FamiliemedlemBuilder.fnr
            this.navn = this@FamiliemedlemBuilder.navn
            this.familierelasjon = this@FamiliemedlemBuilder.familierelasjon
            this.fødselsdato = this@FamiliemedlemBuilder.fødselsdato
            this.borMedBruker = this@FamiliemedlemBuilder.borMedBruker
        }
    }
}
