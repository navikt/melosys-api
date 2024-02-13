package no.nav.melosys.domain.mottatteopplysninger.data


class SelvstendigArbeid {
    var erSelvstendig: Boolean? = null
    var selvstendigForetak: List<SelvstendigForetak> = ArrayList()
    fun hentAlleOrganisasjonsnumre(): List<String> = selvstendigForetak.mapNotNull { it.orgnr }
}
