package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser


class SedGrunnlag : MottatteOpplysningerData() {
    var overgangsregelbestemmelser: List<Overgangsregelbestemmelser> = ArrayList()
    var ytterligereInformasjon: String? = null
}

