package no.nav.melosys.domain.dokument.medlemskap

import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.util.IsoLandkodeKonverterer

class Medlemsperiode : HarPeriode {

    companion object {
        private const val KILDE_LÅNEKASSEN = "LAANEKASSEN"
    }

    var id: Long? = null

    lateinit var periode: Periode

    var type: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodetypeMedl

    var status: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodestatusMedl

    var grunnlagstype: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/GrunnlagMedl

    var land: String? = null // ISO3, https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Landkoder

    var lovvalg: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/LovvalgMedl

    var trygdedekning: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/DekningMedl

    var kildedokumenttype: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildedokumentMedl

    var kilde: String? = null // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildesystemMedl

    override fun getPeriode(): Periode = periode

    fun erKildeLånekassen() = KILDE_LÅNEKASSEN == kilde

    fun hentLandSomIso2(): String? = land?.let { IsoLandkodeKonverterer.tilIso2(it) }

    fun erUnntaksperiode() = "NOR" != land

    fun erMedlemskapsperiode() = "NOR" == land
}
