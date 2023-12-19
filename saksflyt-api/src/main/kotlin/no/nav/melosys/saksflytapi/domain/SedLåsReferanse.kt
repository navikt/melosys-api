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
            this.rinaSaksnummer = it[0]
            this.sedID = it[1]
            this.sedVersjon = it[2]
        }
    }

    override val referanse: String
        get() = rinaSaksnummer

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        if (aktiveLåsReferanser.contains(låsReferanse)) {
            return false
        }
        // Eksisterende tester kjører uten denne, så lag tester som treffer når kode under returnerer false
        return aktiveLåsReferanser.any { SedLåsReferanse(it).referanse == referanse }
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
