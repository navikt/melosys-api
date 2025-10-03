package no.nav.melosys.domain.dokument.sed

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.Arbeidsland
import no.nav.melosys.domain.eessi.melding.Arbeidssted
import no.nav.melosys.domain.kodeverk.*

class SedDokument : SaksopplysningDokument {
    var rinaSaksnummer: String? = null
    var rinaDokumentID: String? = null
    var avsenderLandkode: Landkoder? = null
    var fnr: String? = null
    var lovvalgsperiode: Periode? = null
    var lovvalgBestemmelse: LovvalgBestemmelse? = null
    var lovvalgslandKode: Landkoder? = null
    var unntakFraLovvalgBestemmelse: LovvalgBestemmelse? = null
    var unntakFraLovvalgslandKode: Landkoder? = null
    var sedType: SedType? = null
    var bucType: BucType? = null

    var statsborgerskapKoder: List<String> = listOf()
    var arbeidssteder: List<Arbeidssted> = listOf()
    var arbeidsland: List<Arbeidsland> = listOf()
    var erEndring: Boolean = false

    fun hentLovvalgsperiode() = lovvalgsperiode ?: error("lovvalgsperiode er påkrevd for SedDokument")
    fun hentAvsenderLandkode() = avsenderLandkode ?: error("avsenderLandkode er påkrevd for SedDokument")

    fun opprettInnvilgetLovvalgsperiode(): Lovvalgsperiode {
        val periode = requireNotNull(lovvalgsperiode) {
            "Lovvalgsperiode må være satt for å opprette innvilget periode"
        }

        return Lovvalgsperiode().apply {
            bestemmelse = lovvalgBestemmelse
            fom = periode.fom
            tom = periode.tom
            lovvalgsland = lovvalgslandKode?.let { Land_iso2.valueOf(it.kode) }
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET

            if (erUnntaksperiode()) {
                medlemskapstype = Medlemskapstyper.UNNTATT
                dekning = Trygdedekninger.UTEN_DEKNING
            } else {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                dekning = Trygdedekninger.FULL_DEKNING_EOSFO
            }
        }
    }

    fun erUnntaksperiode(): Boolean = lovvalgslandKode != Landkoder.NO

    fun erMedlemskapsperiode(): Boolean = lovvalgslandKode == Landkoder.NO
}
