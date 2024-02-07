package no.nav.melosys.domain.dokument.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper


class KjoennsType @JsonCreator constructor(
    @JsonProperty("kode") val kode: String
) : AbstraktKodeverkHjelper() {

    override fun hentKodeverkNavn(): FellesKodeverk = FellesKodeverk.KJØNNSTYPER
}
