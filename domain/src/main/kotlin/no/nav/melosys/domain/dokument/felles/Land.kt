package no.nav.melosys.domain.dokument.felles

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.melosys.domain.FellesKodeverk

class Land(
    override var kode: String? = null
) : AbstraktKodeverkHjelper() {

    override fun hentKode(): String = kode ?: error("kode er påkrevd for Land")

    override fun hentKodeverkNavn(): FellesKodeverk = FellesKodeverk.LANDKODER

    override fun toString(): String = kode ?: "kode = null"

    // Opprinnelig basert på EØS-land
    companion object {
        const val BELGIA = "BEL"
        const val BULGARIA = "BGR"
        const val DANMARK = "DNK"
        const val ESTLAND = "EST"
        const val FINLAND = "FIN"
        const val FRANKRIKE = "FRA"
        const val FÆRØYENE = "FRO"
        const val GRØNLAND = "GRL"
        const val HELLAS = "GRC"
        const val IRLAND = "IRL"
        const val ISLAND = "ISL"
        const val ITALIA = "ITA"
        const val KOSOVO = "XXK"
        const val KROATIA = "HRV"
        const val KYPROS = "CYP"
        const val LATVIA = "LVA"
        const val LIECHTENSTEIN = "LIE"
        const val LITAUEN = "LTU"
        const val LUXEMBOURG = "LUX"
        const val MALTA = "MLT"
        const val NEDERLAND = "NLD"
        const val NORGE = "NOR"
        const val POLEN = "POL"
        const val PORTUGAL = "PRT"
        const val ROMANIA = "ROU"
        const val SLOVAKIA = "SVK"
        const val SLOVENIA = "SVN"
        const val SPANIA = "ESP"
        const val STATSLØS = "XXX"
        const val STORBRITANNIA = "GBR"
        const val SVALBARD_OG_JAN_MAYEN = "SJM"
        const val SVERIGE = "SWE"
        const val SVEITS = "CHE"
        const val TSJEKKIA = "CZE"
        const val TYSKLAND = "DEU"
        const val UKJENT = "???"
        const val UNGARN = "HUN"
        const val UNKNOWN = "XUK"
        const val ÅLAND = "ALA"
        const val ØSTERRIKE = "AUT"

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun av(landKodeIso3: String?): Land = Land(landKodeIso3)
    }
}
