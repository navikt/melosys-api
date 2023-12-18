package no.nav.melosys.saksflytapi.domain

import java.util.*
import java.util.regex.Pattern

class SedLåsReferanse(låsReferanse: String) {
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

    val referanse: String
        get() = rinaSaksnummer

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SedLåsReferanse
        return rinaSaksnummer == that.rinaSaksnummer && sedID == that.sedID && sedVersjon == that.sedVersjon
    }

    override fun hashCode(): Int = Objects.hash(rinaSaksnummer, sedID, sedVersjon)

    override fun toString(): String = "${rinaSaksnummer}_${sedID}_${sedVersjon}"

    private fun erGyldigReferanse(referanse: String?): Boolean =
        referanse != null && pattern.matcher(referanse).find()

    companion object {
        private val pattern: Pattern = Pattern.compile("[^_]*_[^_]*_\\d+$")
    }
}
