package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.brev.StandardvedleggType

data class StandardvedleggDto (
    val type: StandardvedleggType,
    val frontendTittel: String,
    val dokumentTittel: String,
)
