package no.nav.melosys.melosysmock.pdl

import org.springframework.stereotype.Component

@Component
class PDLQuerySession {
    companion object {
        const val IDENT_KEY = "IDENT"
    }

    private val data: MutableMap<String, String> = mutableMapOf()

    fun hentIdent() = data[IDENT_KEY]

    fun setIdent(ident: String) {
        data[IDENT_KEY] = ident
    }
}
