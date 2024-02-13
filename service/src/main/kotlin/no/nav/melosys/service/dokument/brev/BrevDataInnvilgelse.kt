package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import java.util.*


class BrevDataInnvilgelse(brevbestillingDto: BrevbestillingDto, saksbehandler: String) : BrevData(brevbestillingDto, saksbehandler) {
    private var anmodningsperiodesvar: AnmodningsperiodeSvar? = null
    var lovvalgsperiode: Lovvalgsperiode? = null
    var arbeidsland: String? = null
    var bostedsland: String? = null
    var hovedvirksomhet: AvklartVirksomhet? = null
    var avklartMaritimType: Maritimtyper? = null
    var trygdemyndighetsland: String? = null
    var vedleggA1: BrevDataA1? = null
    var personNavn: String? = null
    var erArt16UtenArt12: Boolean = false
    var erTuristskip: Boolean = false
    var avklarteMedfolgendeBarn: AvklarteMedfolgendeFamilie? = null

    fun getAnmodningsperiodesvar(): Optional<AnmodningsperiodeSvar> {
        return Optional.ofNullable(anmodningsperiodesvar)
    }

    fun setAnmodningsperiodesvar(anmodningsperiodesvar: AnmodningsperiodeSvar) {
        this.anmodningsperiodesvar = anmodningsperiodesvar
    }
}
