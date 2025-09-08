package no.nav.melosys.domain.arkiv

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

data class Vedlegg(
    val innhold: ByteArray? = null,
    val tittel: String? = null
) {
    val hentInnhold: ByteArray
        get() = innhold ?: error("innhold i Vedlegg kan ikke være null")

    fun erGyldig(): Boolean = ArrayUtils.isNotEmpty(innhold) && StringUtils.isNotEmpty(tittel)

    // Property with 'Array' type in a 'data' class: it is recommended to override 'equals)' and 'hashCode)'
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vedlegg

        if (!innhold.contentEquals(other.innhold)) return false
        if (tittel != other.tittel) return false

        return true
    }

    override fun hashCode(): Int =
        innhold.contentHashCode().let {
            31 * it + tittel.hashCode()
        }
}
