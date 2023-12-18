package no.nav.melosys.saksflytapi.domain

import java.util.*
import java.util.regex.Pattern

class SedLåsReferanse(referanse: String) {
    val referanse: String
    private val sedID: String
    private val sedVersjon: String

    init {
        require(erGyldigReferanse(referanse)) { "$referanse er ikke gyldig SED-referanse" }

        val ref = referanse.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        this.referanse = ref[0]
        this.sedID = ref[1]
        this.sedVersjon = ref[2]
    }

    val identifikator: String
        get() = String.format("%s_%s", sedID, sedVersjon)

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as SedLåsReferanse
        return referanse == that.referanse && sedID == that.sedID && sedVersjon == that.sedVersjon
    }

    override fun hashCode(): Int {
        return Objects.hash(referanse, sedID, sedVersjon)
    }

    override fun toString(): String {
        return String.format("%s_%s_%s", referanse, sedID, sedVersjon)
    }

    companion object {
        private val pattern: Pattern = Pattern.compile("[^_]*_[^_]*_\\d+$")

        fun erGyldigReferanse(referanse: String?): Boolean {
            return referanse != null && pattern.matcher(referanse).find()
        }
    }
}
