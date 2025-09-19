package no.nav.melosys.domain.person

import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse

@JvmRecord
data class PersonMedHistorikk(
    val bostedsadresser: Collection<Bostedsadresse>,
    val dødsfall: Doedsfall?,
    val fødsel: Foedsel?,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val folkeregisterpersonstatuser: Collection<Folkeregisterpersonstatus>,
    val kjønn: KjoennType,
    val kontaktadresser: Collection<Kontaktadresse>,
    val navn: Navn,
    val oppholdsadresser: Collection<Oppholdsadresse>,
    val sivilstand: Collection<Sivilstand>,
    val statsborgerskap: Collection<Statsborgerskap>
) : SaksopplysningDokument