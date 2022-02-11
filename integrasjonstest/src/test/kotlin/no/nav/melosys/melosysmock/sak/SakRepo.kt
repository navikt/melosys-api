package no.nav.melosys.melosysmock.sak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.lang.IllegalArgumentException

object SakRepo {
    val repo: MutableMap<Number, Sak> = mutableMapOf()
    val fagsakNrSakRepo: MutableMap<String, Sak> = mutableMapOf()

    fun leggTilSak(sak: Sak) {
        if (repo[sak.id] != null || fagsakNrSakRepo[sak.fagsakNr] != null) {
            throw IllegalArgumentException("Saksnummer er allerede tilknyttet en arkivsak")
        }

        repo[sak.id] = sak
        fagsakNrSakRepo[sak.fagsakNr] = sak
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sak(
    val id: Long,
    val tema: String,
    val applikasjon: String,
    val fagsakNr: String,
    val aktoerId: String,
)
