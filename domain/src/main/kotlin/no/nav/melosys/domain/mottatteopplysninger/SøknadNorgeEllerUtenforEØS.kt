package no.nav.melosys.domain.mottatteopplysninger

import com.google.common.collect.MoreCollectors
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet


class SøknadNorgeEllerUtenforEØS : MottatteOpplysningerData() {
    var trygdedekning: Trygdedekninger? = null
    var representantIUtlandet: RepresentantIUtlandet? = null

    fun hentArbeidsland(): String {
        return soeknadsland.landkoder.stream().collect(MoreCollectors.onlyElement())
    }
}

