package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.eessi.sed.Adressetype.KONTAKTADRESSE
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3

data class Adresse(
    val adressetype: Adressetype? = null,
    val gateadresse: String? = null,
    val postnr: String? = null,
    val poststed: String? = null,
    val region: String? = null,
    var land: String? = null,
    val tilleggsnavn: String? = null
) {

    fun erGyldigAdresse(): Boolean =
        !gateadresse.isNullOrBlank() && gateadresse != IKKE_TILGJENGELIG &&
            !poststed.isNullOrBlank() && poststed != IKKE_TILGJENGELIG &&
            !land.isNullOrBlank()

    fun tilStrukturertAdresse() = StrukturertAdresse().apply {
        landkode = this@Adresse.land
        gatenavn = this@Adresse.gateadresse
        region = this@Adresse.region
        postnummer = this@Adresse.postnr
        poststed = this@Adresse.poststed
        tilleggsnavn = this@Adresse.tilleggsnavn
    }

    companion object {
        const val IKKE_TILGJENGELIG = "N/A"
        const val UKJENT = "Unknown"
        const val INGEN_FAST_ADRESSE = "No fixed address"

        @JvmStatic
        fun lagAdresse(adressetype: Adressetype?, strukturertAdresse: StrukturertAdresse?): Adresse? {
            strukturertAdresse ?: return null

            return fraStrukturertAdresse(strukturertAdresse).copy(
                adressetype = adressetype,
                land = tilIso3(strukturertAdresse.landkode)
            )
        }

        @JvmStatic
        fun lagAdresseMedBareLandkode(landkode: String?): Adresse = Adresse(
            gateadresse = IKKE_TILGJENGELIG,
            poststed = IKKE_TILGJENGELIG,
            tilleggsnavn = IKKE_TILGJENGELIG,
            land = landkode
        )

        fun lagIkkeFastAdresse(landkode: String?): Adresse = Adresse(
            poststed = INGEN_FAST_ADRESSE,
            land = landkode
        )

        @JvmStatic
        fun lagKontaktadresse(kontaktadresse: Kontaktadresse?): Adresse? {
            kontaktadresse ?: return null

            kontaktadresse.strukturertAdresse?.let { strukturert ->
                return lagAdresse(KONTAKTADRESSE, strukturert)
            }

            return kontaktadresse.semistrukturertAdresse?.let { semistrukturert ->
                lagAdresse(KONTAKTADRESSE, semistrukturert.tilStrukturertAdresse())
            }
        }

        @JvmStatic
        fun lagOppholdsadresse(oppholdsadresse: Oppholdsadresse?): Adresse? {
            // Adressetype POSTADRESSE svarer til opphold i Rina
            return lagAdresse(Adressetype.POSTADRESSE, oppholdsadresse?.strukturertAdresse)
        }

        @JvmStatic
        fun fraStrukturertAdresse(strukturertAdresse: StrukturertAdresse): Adresse = Adresse(
            gateadresse = lagGateadresse(strukturertAdresse.gatenavn, strukturertAdresse.husnummerEtasjeLeilighet),
            tilleggsnavn = strukturertAdresse.tilleggsnavn,
            postnr = strukturertAdresse.postnummer.takeUnless { it.isNullOrBlank() } ?: IKKE_TILGJENGELIG,
            poststed = strukturertAdresse.poststed.takeUnless { it.isNullOrBlank() } ?: UKJENT,
            region = strukturertAdresse.region,
            land = strukturertAdresse.landkode
        )

        private fun lagGateadresse(gatenavn: String?, husnummer: String?): String =
            when {
                gatenavn.isNullOrBlank() -> IKKE_TILGJENGELIG
                husnummer.isNullOrEmpty() -> gatenavn
                else -> "$gatenavn $husnummer"
            }
    }
}
