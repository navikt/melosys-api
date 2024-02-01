package no.nav.melosys.tjenester.gui.dto.periode

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

data class LovvalgsperiodeDto(
    val periodeID: String,
    @JsonUnwrapped(suffix = "Dato") val periode: PeriodeDto,
    val lovvalgsbestemmelse: LovvalgBestemmelse?,
    val tilleggBestemmelse: LovvalgBestemmelse?,
    val lovvalgsland: Land_iso2?,
    val innvilgelsesResultat: InnvilgelsesResultat,
    val trygdeDekning: Trygdedekninger?,
    val medlemskapstype: Medlemskapstyper?,
    val medlemskapsperiodeID: String?
) {
    companion object {
        private val konverterer = LovvalgBestemmelsekonverterer()

        @JvmStatic
        @JsonCreator
        fun fromJson(json: Map<String, String>): LovvalgsperiodeDto {
            return LovvalgsperiodeDto(
                json["periodeID"]!!,
                PeriodeDto(LocalDate.parse(json["fomDato"]),
                if (StringUtils.isEmpty(json["tomDato"])) null else LocalDate.parse(json["tomDato"])),
                konverterLovvalgsBestemmelse(json["lovvalgsbestemmelse"]),
                konverterLovvalgsBestemmelse(json["tilleggBestemmelse"]),
                enumVerdiEllerNull(Land_iso2::class.java, json["lovvalgsland"]),
                InnvilgelsesResultat.valueOf(json["innvilgelsesResultat"] ?: ""),
                enumVerdiEllerNull(Trygdedekninger::class.java, json["trygdeDekning"]),
                enumVerdiEllerNull(Medlemskapstyper::class.java, json["medlemskapstype"]),
                json["medlemskapsperiodeID"]
            )
        }

        @JvmStatic
        fun av(lovvalgsperiode: Lovvalgsperiode): LovvalgsperiodeDto {
            return LovvalgsperiodeDto(
                lovvalgsperiode.id.toString(),
                PeriodeDto(lovvalgsperiode.fom, lovvalgsperiode.tom),
                lovvalgsperiode.bestemmelse,
                lovvalgsperiode.tilleggsbestemmelse,
                lovvalgsperiode.lovvalgsland,
                lovvalgsperiode.innvilgelsesresultat,
                lovvalgsperiode.dekning,
                lovvalgsperiode.medlemskapstype,
                lovvalgsperiode.medlPeriodeID?.toString()
            )
        }

        private fun konverterLovvalgsBestemmelse(bestemmelsesnavn: String?): LovvalgBestemmelse? {
            return konverterer.convertToEntityAttribute(bestemmelsesnavn)
        }

        @JvmStatic
        fun <E : Enum<E>> enumVerdiEllerNull(enumKlasse: Class<E>, nøkkel: String?): E? {
            return nøkkel?.let {
                java.lang.Enum.valueOf(enumKlasse, it)
            }
        }
    }

    fun til(): Lovvalgsperiode {
        val resultat = Lovvalgsperiode()
        resultat.fom = periode.fom
        resultat.tom = periode.tom
        resultat.lovvalgsland = enumVerdiEllerNull(Land_iso2::class.java, lovvalgsland?.name)
        resultat.bestemmelse = konverterer.convertToEntityAttribute(lovvalgsbestemmelse?.name())
        resultat.tilleggsbestemmelse = konverterer.convertToEntityAttribute(tilleggBestemmelse?.name())
        resultat.innvilgelsesresultat = enumVerdiEllerNull(InnvilgelsesResultat::class.java, innvilgelsesResultat?.name)
        resultat.dekning = enumVerdiEllerNull(Trygdedekninger::class.java, trygdeDekning?.name)
        resultat.medlemskapstype = enumVerdiEllerNull(Medlemskapstyper::class.java, medlemskapstype?.name)
        resultat.medlPeriodeID = medlemskapsperiodeID?.toLongOrNull()
        return resultat
    }
}
