package no.nav.melosys.domain.person

import java.util.Objects.nonNull

@JvmRecord
data class Navn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {
    fun tilSammensattNavn(): String = (etternavn + leggTilMellomnavn() + " " + fornavn).trim()

    private fun leggTilMellomnavn(): String = if (mellomnavn == null) "" else " $mellomnavn"

    fun harLiktFornavn(navn: String): Boolean = nonNull(fornavn) && fornavn == navn
}
