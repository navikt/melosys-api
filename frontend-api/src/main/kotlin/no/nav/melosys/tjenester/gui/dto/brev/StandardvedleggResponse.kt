package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.brev.StandardvedleggType

data class StandardvedleggResponse (
    val vedlegg: List<StandardvedleggType>
)
