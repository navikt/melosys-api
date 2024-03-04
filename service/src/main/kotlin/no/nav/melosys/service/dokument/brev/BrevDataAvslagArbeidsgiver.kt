package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.person.Persondata


class BrevDataAvslagArbeidsgiver(
    saksbehandler: String
) : BrevData(saksbehandler = saksbehandler) {
    var person: Persondata? = null
    var hovedvirksomhet: AvklartVirksomhet? = null
    var lovvalgsperiode: Lovvalgsperiode? = null
    var arbeidsland: String? = null

    var vilkårbegrunnelser121: Set<VilkaarBegrunnelse> = emptySet()
    var vilkårbegrunnelser121VesentligVirksomhet: Set<VilkaarBegrunnelse> = emptySet()
}
