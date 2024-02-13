package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.kodeverk.Land_iso2


class AnmodningEllerAttest : MottatteOpplysningerData() {
    var avsenderland: Land_iso2? = null
    var lovvalgsland: Land_iso2? = null
}

