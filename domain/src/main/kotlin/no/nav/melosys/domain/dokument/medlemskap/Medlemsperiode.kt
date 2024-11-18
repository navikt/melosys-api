package no.nav.melosys.domain.dokument.medlemskap

import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.util.IsoLandkodeKonverterer

data class Medlemsperiode(
    var id: Long? = null,
    private var periode: Periode? = null,
    var type: String? = null,
    var status: String? = null,
    var grunnlagstype: String? = null,
    var land: String? = null,
    var lovvalg: String? = null,
    var trygdedekning: String? = null,
    var kildedokumenttype: String? = null,
    var kilde: String? = null
) : HarPeriode {
    companion object {
        private const val KILDE_LÅNEKASSEN = "LAANEKASSEN"
    }

    override fun getPeriode(): Periode? = periode

    fun erKildeLånekassen(): Boolean = kilde == KILDE_LÅNEKASSEN

    fun hentLandSomIso2(): String? = if (land != null) IsoLandkodeKonverterer.tilIso2(land) else null

    fun erUnntaksperiode(): Boolean = land != "NOR"

    fun erMedlemskapsperiode(): Boolean = land == "NOR"
}
