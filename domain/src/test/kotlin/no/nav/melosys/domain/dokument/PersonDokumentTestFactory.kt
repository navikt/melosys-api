package no.nav.melosys.domain.dokument

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.Personstatus
import no.nav.melosys.domain.dokument.person.Sivilstand
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import java.time.LocalDate

object PersonDokumentTestFactory {
    const val FNR = "12345678901"
    const val FORNAVN = "Ola"
    const val ETTERNAVN = "Nordmann"

    @MelosysTestDsl
    class Builder {
        var fnr: String? = FNR
        var fornavn: String? = FORNAVN
        var etternavn: String? = ETTERNAVN
        var fødselsdato: LocalDate? = LocalDate.of(1990, 1, 1)
        var statsborgerskap: Land? = Land("NOR")
        var kjønn: KjoennsType? = KjoennsType("M")
        var sivilstand: Sivilstand? = null
        var personstatus: Personstatus? = null
        var bostedsadresse: Bostedsadresse? = null

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
            }
    }
}
