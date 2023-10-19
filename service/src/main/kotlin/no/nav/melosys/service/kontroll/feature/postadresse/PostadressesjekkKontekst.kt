package no.nav.melosys.service.kontroll.feature.postadresse

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.kodeverk.Aktoersroller


data class PostadressesjekkKontekst(
    val behandlingID: Long?,
    var brukerID: String?,
    var orgnr: String?,
    var rolle: Aktoersroller
) {
    constructor(behandlingID: Long?, brukerID: String?, orgnr: String?) : this(
        behandlingID,
        brukerID,
        orgnr,
        Aktoersroller.BRUKER
    )

    fun oppdaterForBruker(brukerID: String) {
        this.rolle = Aktoersroller.BRUKER
        this.brukerID = brukerID
    }

    fun oppdaterForFullmektigTilBruker(fullmektig: Aktoer) {
        this.rolle = fullmektig.rolle
        if (fullmektig.erPerson()) {
            this.brukerID = fullmektig.personIdent
        }
        if (fullmektig.erOrganisasjon()) {
            this.orgnr = fullmektig.orgnr
        }
    }
}
