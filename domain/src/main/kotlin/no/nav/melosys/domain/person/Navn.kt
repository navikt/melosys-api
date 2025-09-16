package no.nav.melosys.domain.person

import java.util.Objects.nonNull

@JvmRecord
data class Navn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {
    fun tilSammensattNavn(): String {
        return (etternavn + leggTilMellomnavn() + " " + fornavn).trim()
    }

    private fun leggTilMellomnavn(): String {
        return if (mellomnavn == null) "" else " $mellomnavn"
    }

    fun harLiktFornavn(navn: String): Boolean {
        return nonNull(fornavn) && fornavn == navn
    }

    companion object {
        // A B C -> C, A B
        @JvmStatic
        fun navnEtternavnFørst(fulltNavnEtternavnSist: String): String {
            val splittetNavn = fulltNavnEtternavnSist.split(" ")
            val etternavn = splittetNavn[splittetNavn.size - 1]
            val forOgMellomnavn = splittetNavn.take(splittetNavn.size - 1).joinToString(" ")
            return "$etternavn, $forOgMellomnavn"
        }

        // C, A B -> A B C
        @JvmStatic
        fun navnEtternavnSist(fulltNavnEtternavnFørst: String): String {
            if (!fulltNavnEtternavnFørst.contains(",")) return fulltNavnEtternavnFørst

            val splittetNavn = fulltNavnEtternavnFørst.split(" ")
            val etternavn = splittetNavn[0].substring(0, splittetNavn[0].length - 1)
            val forOgMellomnavn = splittetNavn.drop(1).joinToString(" ")
            return "$forOgMellomnavn $etternavn"
        }
    }
}