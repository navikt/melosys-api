package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.person.Foedsel
import no.nav.melosys.domain.person.Folkeregisteridentifikator
import no.nav.melosys.domain.person.Master
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.familie.Familiemedlem
import java.time.LocalDate


class Familiemedlem {
    var fnr: String? = null
    var navn: String? = null
    var familierelasjon: Familierelasjon? = null
    var fødselsdato: LocalDate? = null
    var borMedBruker = false
    var sivilstand: Sivilstand? = null
    var sivilstandGyldighetsperiodeFom: LocalDate? = null
    var fnrAnnenForelder: String? = null

    fun erForelder(): Boolean {
        return (familierelasjon === Familierelasjon.FARA
            || familierelasjon === Familierelasjon.MORA)
    }

    fun tilDomene(): Familiemedlem {
        val splittetNavn = splitFulltNavn(navn)
        return Familiemedlem(
            fnr?.let { Folkeregisteridentifikator(it) },
            lagNavn(splittetNavn),
            mapFamilierelasjon(familierelasjon),
            Foedsel(fødselsdato, null, null, null),
            fnrAnnenForelder?.let { Folkeregisteridentifikator(it) },
            null,
            if (sivilstand == null) null else lagSivilstand(sivilstand!!, sivilstandGyldighetsperiodeFom)
        )
    }

    private fun lagSivilstand(sivilstand: Sivilstand, gyldighetsperiodeFom: LocalDate?): no.nav.melosys.domain.person.Sivilstand {
        return no.nav.melosys.domain.person.Sivilstand(
            sivilstand.tilSivilstandstypeFraDomene(), sivilstand.kode, "",
            gyldighetsperiodeFom, null, Master.TPS.name, Master.TPS.name, false
        )
    }

    private fun mapFamilierelasjon(familierelasjon: Familierelasjon?): no.nav.melosys.domain.person.familie.Familierelasjon {
        return when (familierelasjon) {
            Familierelasjon.BARN -> no.nav.melosys.domain.person.familie.Familierelasjon.BARN
            Familierelasjon.EKTE, Familierelasjon.REPA, Familierelasjon.SAM -> no.nav.melosys.domain.person.familie.Familierelasjon.RELATERT_VED_SIVILSTAND
            Familierelasjon.FARA -> no.nav.melosys.domain.person.familie.Familierelasjon.FAR
            Familierelasjon.MORA -> no.nav.melosys.domain.person.familie.Familierelasjon.MOR
            null -> throw IllegalArgumentException("Familierelasjon mangler")
        }
    }

    private fun lagNavn(splittetNavn: Array<String?>): Navn {
        return if (splittetNavn.size > 2) Navn(splittetNavn[0], splittetNavn[1], splittetNavn[2]) else Navn(
            splittetNavn[0], null, splittetNavn[1]
        )
    }

    companion object {
        private fun splitFulltNavn(navn: String?): Array<String?> {
            return if (navn == null || navn.isEmpty()) {
                arrayOfNulls(2)
            } else if (!navn.contains(" ")) {
                arrayOf(navn, null)
            } else {
                navn.split(" ".toRegex(), limit = 3).toTypedArray()
            }
        }
    }
}
