package no.nav.melosys.domain.mottatteopplysninger.data

import java.util.*


class UtenlandskIdent {
    var ident: String? = null
    var landkode: String? = null

    constructor()
    constructor(ident: String?, landkode: String?) {
        this.ident = ident
        this.landkode = landkode
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as UtenlandskIdent
        return ident == that.ident && landkode == that.landkode
    }

    override fun hashCode(): Int {
        return Objects.hash(ident, landkode)
    }
}

