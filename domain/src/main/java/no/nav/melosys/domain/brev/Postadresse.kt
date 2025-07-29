package no.nav.melosys.domain.brev

import no.nav.melosys.domain.adresse.Adresse.Companion.sammenslå
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse

@JvmRecord
data class Postadresse(
    val coAdressenavn: String?,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val adresselinje4: String?,
    val postnr: String?,
    val poststed: String?,
    val landkode: String?,
    val region: String?
) {
    fun adresselinjer(): List<String> {
        val adresselinjer = mutableListOf<String>()
        coAdressenavn?.let { adresselinjer.add(it) }
        adresselinjer.addAll(listOfNotNull(
            adresselinje1,
            adresselinje2,
            adresselinje3,
            adresselinje4
        ))
        return adresselinjer
    }

    companion object {
        fun lagPostadresse(coAdressenavn: String?, strukturertAdresse: StrukturertAdresse) = Postadresse(
            coAdressenavn = coAdressenavn,
            adresselinje1 = sammenslå(strukturertAdresse.gatenavn, strukturertAdresse.husnummerEtasjeLeilighet),
            adresselinje2 = lagPostboksAddresselinje(strukturertAdresse.postboks),
            adresselinje3 = null,
            adresselinje4 = null,
            postnr = strukturertAdresse.postnummer,
            poststed = strukturertAdresse.poststed,
            landkode = strukturertAdresse.landkode,
            region = strukturertAdresse.region
        )

        private fun lagPostboksAddresselinje(postboks: String?) =
            postboks?.let { "Postboks $it" }

        fun lagPostadresse(coAdressenavn: String?, semistrukturertAdresse: SemistrukturertAdresse) = Postadresse(
            coAdressenavn = coAdressenavn,
            adresselinje1 = semistrukturertAdresse.adresselinje1,
            adresselinje2 = semistrukturertAdresse.adresselinje2,
            adresselinje3 = semistrukturertAdresse.adresselinje3,
            adresselinje4 = null,
            postnr = semistrukturertAdresse.postnr,
            poststed = semistrukturertAdresse.poststed,
            landkode = semistrukturertAdresse.landkode,
            region = null
        )
    }
}
