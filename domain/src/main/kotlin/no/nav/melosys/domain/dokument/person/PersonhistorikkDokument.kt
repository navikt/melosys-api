package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse
import no.nav.melosys.domain.dokument.person.adresse.PostadressePeriode

data class PersonhistorikkDokument(
    var statsborgerskapListe: List<StatsborgerskapPeriode> = ArrayList(),
    var bostedsadressePeriodeListe: List<BostedsadressePeriode> = ArrayList(),
    var postadressePeriodeListe: List<PostadressePeriode> = ArrayList(),
    var midlertidigAdressePeriodeListe: List<MidlertidigPostadresse> = ArrayList()
) : SaksopplysningDokument
