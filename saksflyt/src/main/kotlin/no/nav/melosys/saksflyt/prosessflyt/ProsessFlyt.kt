package no.nav.melosys.saksflyt.prosessflyt

import jakarta.annotation.Nullable
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType


class ProsessFlyt internal constructor(prosessType: ProsessType, vararg prosessSteg: ProsessSteg) {
    private val prosessType: ProsessType
    private val stegListe: List<ProsessSteg>

    init {
        val prosessStegListe = ArrayList<ProsessSteg>()

        for (steg in prosessSteg) {
            require(!prosessStegListe.contains(steg)) { "Prosessteg $steg er definert to eller flere ganger!" }
            prosessStegListe.add(steg)
        }

        this.prosessType = prosessType
        this.stegListe = java.util.List.copyOf(prosessStegListe)
    }

    @Nullable
    fun nesteSteg(forrigeSteg: ProsessSteg?): ProsessSteg? {
        val iter = stegListe.iterator()

        if (forrigeSteg == null) return iter.next()

        while (iter.hasNext()) {
            val s = iter.next()
            if (s == forrigeSteg) return if (iter.hasNext()) iter.next() else null
        }

        throw IllegalArgumentException("Forrige steg $forrigeSteg er ikke gyldig for prosesstype $prosessType")
    }
}
