package no.nav.melosys.domain.mottatteopplysninger.data

import java.util.stream.Stream


class SelvstendigArbeid {
    var erSelvstendig: Boolean? = null
    var selvstendigForetak: List<SelvstendigForetak> = ArrayList()
    fun hentAlleOrganisasjonsnumre(): Stream<String?> {
        return selvstendigForetak.stream().map { sf: SelvstendigForetak -> sf.orgnr }
    }
}
