package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.melding.Arbeidsland
import no.nav.melosys.domain.eessi.melding.Arbeidssted
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet

object ArbeidsstedRegler {

    private const val BYER_FRA_SVALBARD_REGEX =
        "(Ny-Ålesund)|(Ny-Aalesund)|(Ny-Alesund)|(Ny Ålesund)|(Ny Aalesund)|(Ny Alesund)|(Svalbard)|(Sveagruva)|" +
            "(Bjørnøya)|(Bjoernoya)|(Bjornoya)|(Spitsbergen)|(Longyearbyen)|(\\bHopen\\b)"

    private val BYER_FRA_SVALBARD_PATTERN = Regex(BYER_FRA_SVALBARD_REGEX, RegexOption.IGNORE_CASE)

    private val ARBEIDSSTED_SVALBARD_JAN_MAIEN: (Arbeidssted?) -> Boolean = { arbeidssted ->
        arbeidssted?.let {
            val by = it.adresse.by ?: return@let false
            (it.adresse.land == Landkoder.SJ.kode || (it.adresse.land == Landkoder.NO.kode && BYER_FRA_SVALBARD_PATTERN.containsMatchIn(by)))
        } ?: false
    }

    private val ARBEIDSLAND_ARBEIDSSTED_SVALBARD_JAN_MAIEN: (Arbeidsland) -> Boolean = { arbeidsland ->
        arbeidsland.land == Landkoder.SJ.kode ||
            (arbeidsland.land == Landkoder.NO.kode &&
                arbeidsland.arbeidssted?.any { arbeidssted ->
                    arbeidssted?.let {
                        val by = it.adresse.by ?: return@let false
                        BYER_FRA_SVALBARD_PATTERN.containsMatchIn(by)
                    } ?: false
                } ?: false)
    }

    @JvmStatic
    fun representantIUtlandetMangler(representantIUtlandet: RepresentantIUtlandet?): Boolean {
        return representantIUtlandet?.representantNavn == null
    }

    @JvmStatic
    fun erArbeidsstedFraSvalbardOgJanMayen(sedDokument: SedDokument): Boolean {
        return sedDokument.arbeidssteder?.any(ARBEIDSSTED_SVALBARD_JAN_MAIEN) ?: false
    }

    @JvmStatic
    fun erArbeidsstedFraSvalbardOgJanMayen4_3(sedDokument: SedDokument): Boolean {
        return sedDokument.arbeidsland.any(ARBEIDSLAND_ARBEIDSSTED_SVALBARD_JAN_MAIEN)
    }
}
