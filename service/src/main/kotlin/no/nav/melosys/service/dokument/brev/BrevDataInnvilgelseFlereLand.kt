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
    var ukjenteEllerAlleEosLand: Boolean = false
    var lovvalgsperiode: Lovvalgsperiode? = null
    var avklartMaritimTypeSokkel: Boolean = false
    var avklartMaritimTypeSkip: Boolean = false
    var bostedsland: String? = null
    var begrensetPeriode: Boolean = false
    var marginaltArbeid: Boolean = false
    var trydemyndighetsland: Landkoder? = null

    var vedleggA1: BrevDataA1? = null
}
