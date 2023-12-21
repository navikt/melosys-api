package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

class SedLåsReferanse(val låsReferanse: String) : LåsReferanse {
    val rinaSaksnummer: String
    val sedID: String
    val sedVersjon: String

    init {
        require(erGyldigReferanse(låsReferanse)) { "$låsReferanse er ikke gyldig SED-referanse" }
        låsReferanse.split("_").let {
            rinaSaksnummer = it[0]
            sedID = it[1]
            sedVersjon = it[2]
        }
    }

    override val referanse: String
        get() = rinaSaksnummer

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        // er ikke logisk at vi ikke setter en SED på vent om det finnes en annen samme referanse
        // Men dette må sees på i en egen oppgave
        return !aktiveLåsReferanser.contains(låsReferanse)
    }

    override fun toString(): String = låsReferanse

    private fun erGyldigReferanse(referanse: String): Boolean =
        LåsReferanseType.SED.erGyldigReferanse(referanse)
}
