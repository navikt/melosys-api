package no.nav.melosys.domain.dokument.medlemskap

import no.nav.melosys.domain.dokument.SaksopplysningDokument

class MedlemskapDokument : SaksopplysningDokument {

    var medlemsperiode: List<Medlemsperiode> = emptyList()

    fun hentMedlemsperioderHvorKildeIkkeLånekassen(): List<Medlemsperiode> =
        medlemsperiode.filterNot { it.erKildeLånekassen() }
}
