package no.nav.melosys.domain.adresse

import no.nav.melosys.domain.kodeverk.Landkoder

interface Adresse {
    val landkode: String?
    fun erTom(): Boolean
    fun toList(): List<String?>

    fun erNorsk(): Boolean = landkode == Landkoder.NO.kode

    companion object {
        @JvmStatic
        fun sammenslå(vararg strings: String?): String =
            strings
                .filterNotNull()
                .joinToString(" ")
                .trim()
    }
}
