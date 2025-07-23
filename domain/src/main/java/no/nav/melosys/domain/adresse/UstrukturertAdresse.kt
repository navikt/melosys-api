package no.nav.melosys.domain.adresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.adresse.Adresse.Companion.sammenslå
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland
import org.apache.commons.lang3.StringUtils

class UstrukturertAdresse(
    l1: String?,
    l2: String?,
    l3: String?,
    l4: String?,
    override val landkode: String?
) : Adresse {

    private val adresselinjer: MutableList<String> = listOfNotNull(
        l1.takeIf { StringUtils.isNotEmpty(it) },
        l2.takeIf { StringUtils.isNotEmpty(it) },
        l3.takeIf { StringUtils.isNotEmpty(it) },
        l4.takeIf { StringUtils.isNotEmpty(it) }
    ).toMutableList()

    companion object {
        @JvmStatic
        fun av(adresse: MidlertidigPostadresseUtland): UstrukturertAdresse =
            UstrukturertAdresse(
                adresse.adresselinje1,
                adresse.adresselinje2,
                adresse.adresselinje3,
                adresse.adresselinje4,
                adresse.land?.kode
            )

        @JvmStatic
        fun av(sAdresse: SemistrukturertAdresse): UstrukturertAdresse {
            val poststed = if (sAdresse.erUtenlandsk()) {
                sAdresse.poststedUtland
            } else {
                val suffix = sAdresse.poststed?.let { " $it" } ?: ""
                sAdresse.postnr + suffix
            }

            return UstrukturertAdresse(
                sAdresse.adresselinje1,
                sAdresse.adresselinje2,
                sAdresse.adresselinje3,
                poststed,
                sAdresse.landkode
            )
        }

        @JvmStatic
        fun av(adresse: no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse): UstrukturertAdresse =
            UstrukturertAdresse(
                adresse.adresselinje1,
                adresse.adresselinje2,
                adresse.adresselinje3,
                adresse.adresselinje4,
                adresse.land?.kode
            ).apply {
                adresselinjer.add(sammenslå(adresse.postnr, adresse.poststed))
            }

        @JvmStatic
        fun av(sAdresse: StrukturertAdresse): UstrukturertAdresse =
            UstrukturertAdresse(
                sammenslå(sAdresse.tilleggsnavn, sAdresse.gatenavn, sAdresse.husnummerEtasjeLeilighet),
                sAdresse.postboks,
                sammenslå(sAdresse.postnummer, sAdresse.poststed),
                sAdresse.region,
                sAdresse.landkode
            )
    }

    @JsonIgnore
    fun getAdresselinje(linjeNummer: Int): String? =
        if (linjeNummer > adresselinjer.size) null else adresselinjer[linjeNummer - 1]

    override fun erTom(): Boolean = adresselinjer.isEmpty() && landkode.isNullOrEmpty()

    override fun toList(): List<String> =
        listOfNotNull(
            getAdresselinje(1),
            getAdresselinje(2),
            getAdresselinje(3),
            getAdresselinje(4),
            landkode
        )

    override fun toString(): String = toList().joinToString(", ")
}
