package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import no.nav.melosys.domain.kodeverk.Flyvningstyper


data class LuftfartBase @JvmOverloads constructor(
    var hjemmebaseNavn: String? = null,
    var hjemmebaseLand: String? = null,
    var typeFlyvninger: Flyvningstyper? = null,
    var erVanligHjemmebase: Boolean? = null,
    var vanligHjemmebaseLand: String? = null,
    var vanligHjemmebaseNavn: String? = null
)
