package no.nav.melosys.domain.person

import no.nav.melosys.domain.brev.Postadresse
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.familie.Familiemedlem
import java.time.LocalDate
import java.util.*

interface Persondata : SaksopplysningDokument {
    fun erPersonDød(): Boolean

    fun harStrengtAdressebeskyttelse(): Boolean

    fun manglerGyldigRegistrertAdresse(): Boolean

    fun hentFolkeregisterident(): String?

    fun hentAlleStatsborgerskap(): Set<Land?>

    fun hentKjønnType(): KjoennType?

    val fornavn: String?

    val mellomnavn: String?

    val etternavn: String?

    val sammensattNavn: String?

    fun hentFamiliemedlemmer(): Set<Familiemedlem>?

    val fødselsdato: LocalDate?

    fun finnBostedsadresse(): Optional<Bostedsadresse>

    fun finnKontaktadresse(): Optional<Kontaktadresse>

    fun finnOppholdsadresse(): Optional<Oppholdsadresse>

    fun hentGjeldendePostadresse(): Postadresse?
}
