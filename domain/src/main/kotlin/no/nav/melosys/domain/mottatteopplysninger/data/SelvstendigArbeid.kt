package no.nav.melosys.domain.mottatteopplysninger.data


class SelvstendigArbeid {
    var erSelvstendig: Boolean? = null
    var selvstendigForetak: List<SelvstendigForetak> = ArrayList()
    fun hentAlleOrganisasjonsnumre(): List<String?> {
        return selvstendigForetak.map { it.orgnr }
    }
}
