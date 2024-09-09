package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.math.BigDecimal
import java.time.LocalDate

data class ÅrsavregningModel(
    val år: Int,
    val tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal?,
    val nyttTotalbeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
    val erFørstegangsÅrsavregning: Boolean = true
) {
    companion object {
        fun lagÅrsavregningModelFraÅrsavregning(
            årsavregning: Årsavregning,
            tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
            nyttGrunnlag: Trygdeavgiftsgrunnlag?,
            erFørstegangsÅrsavregning: Boolean = true
        ): ÅrsavregningModel {
            return ÅrsavregningModel(
                år = årsavregning.aar,
                tidligereGrunnlag = tidligereGrunnlag,
                tidligereAvgift = årsavregning.tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(årsavregning.aar) }
                    .orEmpty(),
                nyttGrunnlag = nyttGrunnlag,
                endeligAvgift = årsavregning.behandlingsresultat.trygdeavgiftsperioder.toList(),
                tidligereFakturertBeloep = årsavregning.tidligereFakturertBeloep,
                nyttTotalbeloep = årsavregning.nyttTotalbeloep,
                tilFaktureringBeloep = årsavregning.tilFaktureringBeloep,
                erFørstegangsÅrsavregning = erFørstegangsÅrsavregning
            )
        }
    }
}

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<MedlemskapsperiodeForAvgift>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val innteksperioder: List<Inntektsperiode>
)

data class MedlemskapsperiodeForAvgift(
    val fom: LocalDate,
    val tom: LocalDate,
    val dekning: Trygdedekninger,
    val bestemmelse: Folketrygdloven_kap2_bestemmelser,
    val medlemskapstyper: Medlemskapstyper
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
}
