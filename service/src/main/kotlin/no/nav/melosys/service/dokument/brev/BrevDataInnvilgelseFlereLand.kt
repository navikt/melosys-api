package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Landkoder

class BrevDataInnvilgelseFlereLand(
    brevbestillingDto: BrevbestillingDto,
    saksbehandler: String
) : BrevData(brevbestillingDto, saksbehandler) {
    var arbeidsgivere: Collection<AvklartVirksomhet> = emptyList()
    var alleArbeidsland: List<String> = emptyList()
    var erUkjenteEllerAlleEosLand: Boolean = false
    var lovvalgsperiode: Lovvalgsperiode? = null
    var harAvklartMaritimTypeSokkel: Boolean = false
    var harAvklartMaritimTypeSkip: Boolean = false
    var bostedsland: String? = null
    var erBegrensetPeriode: Boolean = false
    var erMarginaltArbeid: Boolean = false
    var trydemyndighetsland: Landkoder? = null

    var vedleggA1: BrevDataA1? = null
}
