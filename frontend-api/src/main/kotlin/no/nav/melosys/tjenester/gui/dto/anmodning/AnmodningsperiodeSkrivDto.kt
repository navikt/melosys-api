package no.nav.melosys.tjenester.gui.dto.anmodning

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

open class AnmodningsperiodeSkrivDto(
    val id: String,
    @JsonUnwrapped(suffix = "Dato") val periode: PeriodeDto,
    val lovvalgBestemmelse: LovvalgBestemmelse?,
    val tilleggBestemmelse: LovvalgBestemmelse?,
    val lovvalgsland: Land_iso2?,
    val unntakFraBestemmelse: LovvalgBestemmelse?,
    val unntakFraLovvalgsland: Land_iso2?,
    val trygdeDekning: Trygdedekninger?,
    val medlemskapsperiodeID: String?,
) {
    companion object {
        private val konverterer = LovvalgBestemmelsekonverterer()

        @JvmStatic
        @JsonCreator
        fun fromJson(json: Map<String, String>): AnmodningsperiodeSkrivDto {
            return AnmodningsperiodeSkrivDto(
                json["id"]!!,
                PeriodeDto(
                    LocalDate.parse(json["fomDato"]),
            if (StringUtils.isEmpty(json["tomDato"])) null else LocalDate.parse(json["tomDato"])
                ),
                konverterLovvalgsBestemmelse(json["lovvalgBestemmelse"]),
                konverterLovvalgsBestemmelse(json["tilleggBestemmelse"]),
                enumVerdiEllerNull(Land_iso2::class.java, json["lovvalgsland"]),
                konverterLovvalgsBestemmelse(json["unntakFraBestemmelse"]),
                enumVerdiEllerNull(Land_iso2::class.java, json["unntakFraLovvalgsland"]),
                enumVerdiEllerNull(Trygdedekninger::class.java, json["trygdeDekning"]),
                json["medlemskapsperiodeID"]
            )
        }

        fun av(anmodningsperiode: Anmodningsperiode): AnmodningsperiodeSkrivDto {
            return AnmodningsperiodeSkrivDto(
                anmodningsperiode.id.toString(),
                PeriodeDto(anmodningsperiode.fom, anmodningsperiode.tom),
                anmodningsperiode.bestemmelse,
            anmodningsperiode.tilleggsbestemmelse,
                anmodningsperiode.lovvalgsland,
                anmodningsperiode.unntakFraBestemmelse,
                anmodningsperiode.unntakFraLovvalgsland,
                anmodningsperiode.dekning,
                anmodningsperiode.medlPeriodeID?.toString()
            )
        }

        private fun konverterLovvalgsBestemmelse(bestemmelsesnavn: String?): LovvalgBestemmelse? {
            return konverterer.convertToEntityAttribute(bestemmelsesnavn)
        }

        private fun <E : Enum<E>> enumVerdiEllerNull(enumKlasse: Class<E>, nøkkel: String?): E? {
            return nøkkel?.let {
                java.lang.Enum.valueOf(enumKlasse, it)
            }
        }
    }

    fun til(): Anmodningsperiode {
        return Anmodningsperiode(
            periode.fom,
            periode.tom,
            enumVerdiEllerNull(Land_iso2::class.java, lovvalgsland?.name),
            konverterer.convertToEntityAttribute(lovvalgBestemmelse?.name()),
            konverterer.convertToEntityAttribute(tilleggBestemmelse?.name()),
            enumVerdiEllerNull(Land_iso2::class.java, unntakFraLovvalgsland?.name),
            konverterer.convertToEntityAttribute(unntakFraBestemmelse?.name()),
            enumVerdiEllerNull(Trygdedekninger::class.java, trygdeDekning?.name)
        )
    }
}
