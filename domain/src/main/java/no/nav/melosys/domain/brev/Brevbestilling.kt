package no.nav.melosys.domain.brev

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

abstract class Brevbestilling {
    var produserbartdokument: Produserbaredokumenter? = null
        protected set
    var behandling: Behandling? = null
    var avsenderID: String? = null
        protected set

    // Empty constructor for deserialization in process instance
    protected constructor()

    protected constructor(
        produserbartdokument: Produserbaredokumenter?,
        behandling: Behandling?,
        avsenderID: String?
    ) {
        this.produserbartdokument = produserbartdokument
        this.behandling = behandling
        this.avsenderID = avsenderID
    }

    fun behandlingNonNull(): Behandling = behandling ?: throw IllegalStateException("Behandling kan ikke være null")
}
