package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import no.nav.melosys.domain.kodeverk.Flyvningstyper


data class LuftfartBase(
    var hjemmebaseNavn: String? = null,
    var hjemmebaseLand: String? = null,
    var typeFlyvninger: Flyvningstyper? = null
)
