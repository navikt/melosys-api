package no.nav.melosys.tjenester.gui.dto.kontroller

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.kodeverk.Aktoersroller

data class AdressesjekkKontekst(
    var behandlingID: Long?,
    var brukerID: String?,
    var orgnr: String?,
    var rolle: Aktoersroller = Aktoersroller.BRUKER
) {
    constructor(dto: KontrollerAdresseBrukerFullmektigDto) : this(
        behandlingID = dto.behandlingID,
        brukerID = dto.brukerID,
        orgnr = dto.orgnr
    )

    fun oppdaterForRepresentantBruker(representant: Aktoer) {
        this.rolle = Aktoersroller.REPRESENTANT
        if (representant.erPerson()) {
            this.brukerID = representant.getPersonIdent()
        }
        if (representant.erOrganisasjon()) {
            this.orgnr = representant.getOrgnr()
        }
    }

    fun oppdaterForBruker(brukerID: String) {
        this.rolle = Aktoersroller.BRUKER
        this.brukerID = brukerID
    }
}

