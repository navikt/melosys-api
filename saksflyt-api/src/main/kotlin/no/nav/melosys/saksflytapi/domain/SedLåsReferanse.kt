package no.nav.melosys.saksflytapi.domain

import java.util.*
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
        // Sjekk at aktiveLåsReferanser er hentet med forventet prefix som er rinaSaksnummer
        aktiveLåsReferanser.find { SedLåsReferanse(it).referanse != referanse }?.let {
            throw IllegalStateException("Fant aktiv låsreferanse($it) med en forsjellig rinaSaksnummer: $referanse")
        }

        // er ikke logisk at vi ikke setter en SED på vent om det finnes en annen samme referanse
        // Men dette må sees på i en egen oppgave

        return !aktiveLåsReferanser.contains(låsReferanse)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SedLåsReferanse
        return rinaSaksnummer == that.rinaSaksnummer && sedID == that.sedID && sedVersjon == that.sedVersjon
    }
    override fun hashCode(): Int = Objects.hash(rinaSaksnummer, sedID, sedVersjon)

    override fun toString(): String = låsReferanse

    private fun erGyldigReferanse(referanse: String?): Boolean =
        referanse != null && pattern.matcher(referanse).find()

    companion object {
        private val pattern: Pattern = Pattern.compile("[^_]*_[^_]*_\\d+$")
    }
}
