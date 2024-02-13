package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper

class BrevDataAvslagYrkesaktiv(
    brevbestillingDto: BrevbestillingDto,
    saksbehandler: String
) : BrevData(brevbestillingDto, saksbehandler) {
    var arbeidsland: String? = null
    var hovedvirksomhet: AvklartVirksomhet? = null
    var yrkesaktivitet: Yrkesaktivitetstyper? = null
    var anmodningsperiodeSvar: AnmodningsperiodeSvar? = null // Kotlin uses nullability instead of Optional
    var art16Vilkaar: Vilkaarsresultat? = null
    var erArt16UtenArt12: Boolean = false
}
