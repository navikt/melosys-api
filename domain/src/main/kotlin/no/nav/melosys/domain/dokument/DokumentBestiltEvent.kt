package no.nav.melosys.domain.dokument

import no.nav.melosys.domain.BehandlingEvent
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

class DokumentBestiltEvent(
    behandlingID: Long,
    val produserbaredokumenter: Produserbaredokumenter?
) : BehandlingEvent(behandlingID)
