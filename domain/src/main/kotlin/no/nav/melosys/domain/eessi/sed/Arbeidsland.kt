package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

class Arbeidsland {
    var land: String? = null

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var arbeidssted: List<Arbeidssted> = emptyList()

    constructor(land: String?, arbeidssted: List<Arbeidssted>) {
        this.land = land
        this.arbeidssted = arbeidssted
    }

    fun harFastArbeidssted(): Boolean =
        arbeidssted.any { arbSted -> arbSted.adresse?.erGyldigAdresse() == true }
}
