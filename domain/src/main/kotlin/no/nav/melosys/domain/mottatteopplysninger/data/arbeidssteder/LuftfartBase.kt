package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import no.nav.melosys.domain.kodeverk.Flyvningstyper


class LuftfartBase {
    var hjemmebaseNavn: String? = null
    var hjemmebaseLand: String? = null
    var typeFlyvninger: Flyvningstyper? = null

    constructor()
    constructor(hjemmebaseNavn: String?, hjemmebaseLand: String?, flyvningstype: Flyvningstyper?) {
        this.hjemmebaseNavn = hjemmebaseNavn
        this.hjemmebaseLand = hjemmebaseLand
        typeFlyvninger = flyvningstype
    }
}
