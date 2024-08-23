package no.nav.melosys.saksflyt.prosessflyt

import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType


class ProsessFlyt internal constructor(prosessType: ProsessType, vararg prosessSteg: ProsessSteg) {
    private val prosessType: ProsessType
    private val stegListe: List<ProsessSteg>

    init {
        val duplikater = prosessSteg.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        require(duplikater.isEmpty()) { "Prosessteg $duplikater er definert to eller flere ganger!" }

        this.prosessType = prosessType
        this.stegListe = prosessSteg.toList()
    }

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
