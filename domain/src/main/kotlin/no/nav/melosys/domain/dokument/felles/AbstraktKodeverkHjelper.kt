package no.nav.melosys.domain.dokument.felles

abstract class AbstraktKodeverkHjelper : KodeverkHjelper {
    override var kode: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstraktKodeverkHjelper) return false
        return kode == other.kode
    }

    override fun hashCode(): Int = kode.hashCode()
}
