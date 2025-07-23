package no.nav.melosys.domain.adresse

import org.apache.commons.lang3.StringUtils

@JvmRecord
data class SemistrukturertAdresse(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val adresselinje4: String?,
    val postnr: String?,
    val poststed: String?,
    override val landkode: String?
) : Adresse {

    override fun erTom(): Boolean =
        StringUtils.isAllEmpty(adresselinje1, adresselinje2, adresselinje3, adresselinje4, postnr, poststed, landkode)

    override fun toList(): List<String?> = tilStrukturertAdresse().toList()

    fun tilStrukturertAdresse(): StrukturertAdresse =
        StrukturertAdresse(
            gatenavn = Adresse.sammenslå(adresselinje1, adresselinje2, adresselinje3, adresselinje4),
            postnummer = postnr,
            poststed = this@SemistrukturertAdresse.poststed,
            landkode = this@SemistrukturertAdresse.landkode
        )
}
