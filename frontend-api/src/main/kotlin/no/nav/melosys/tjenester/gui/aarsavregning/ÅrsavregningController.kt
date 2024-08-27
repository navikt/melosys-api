package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.aarsavregning.TrygdeavgiftTotalBeregner
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.InntekskildeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.SkatteforholdTilNorgeDto
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.MedlemskapsperiodeDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@Protected
@RestController
@Api(tags = ["årsavregning", "trygdeavgift"])
@RequestMapping("/aarsavregninger")
class ÅrsavregningController(
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftTotalBeregner: TrygdeavgiftTotalBeregner
) {

    @GetMapping("/{behandlingID}")
    fun hentAvregning(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<ÅrsavregningResponse?> {
        val årsavregning = årsavregningService.finnÅrsavregning(behandlingID) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PostMapping("/{behandlingID}")
    fun opprettNyÅrsavregning(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody årsavregningRequest: LagÅrsavregningRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        val årsavregning = årsavregningService.opprettÅrsavregning(behandlingID, årsavregningRequest.aar)

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    data class LagÅrsavregningRequest(
        val aar: Int
    )

    private fun lagÅrsavregningResponse(årsavregningModel: ÅrsavregningModel) =
        ÅrsavregningResponse(
            aar = årsavregningModel.år,
            tidligereGrunnlagsopplysninger = hentGrunnlagsopplysninger(årsavregningModel.tidligereGrunnlag, årsavregningModel.tidligereAvgift),
            avvikFunnet = årsavregningModel.nyttGrunnlag != null,
            nyttGrunnlag = hentGrunnlagsopplysninger(årsavregningModel.nyttGrunnlag, årsavregningModel.endeligAvgift),
            endeligAvgift = null,
            avregning = AvregningDto(
                nyttTotalbeloep = årsavregningModel.nyttTotalbeloep,
                tidligereFakturertBeloep = årsavregningModel.tidligereFakturertBeloep,
                tilFaktureringBeloep = årsavregningModel.tilFaktureringBeloep,
            )
        )

    private fun mapTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?) =
        TrygdeavgiftsgrunnlagDto(
            medlemskapsperioder = trygdeavgiftsgrunnlag?.medlemskapsperioder?.map {
                MedlemskapsperiodeDto(
                    0,
                    it.fom,
                    it.tom,
                    it.bestemmelse,
                    InnvilgelsesResultat.INNVILGET,
                    it.dekning,
                    it.medlemskapstyper
                )
            }
                .orEmpty(),
            skatteforholdsperioder = trygdeavgiftsgrunnlag?.skatteforholdsperioder?.map {
                SkatteforholdTilNorgeDto(
                    it.fom,
                    it.tom,
                    it.skatteplikttype
                )
            }.orEmpty(),
            inntektskperioder = trygdeavgiftsgrunnlag?.innteksperioder?.map {
                InntekskildeDto(
                    it.type,
                    it.isArbeidsgiversavgiftBetalesTilSkatt,
                    it.avgiftspliktigInntektMnd.verdi,
                    it.fom,
                    it.tom,
                )
            }.orEmpty()
        )


    private fun hentGrunnlagsopplysninger(
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): GrunnlagsOpplysningerDto? {
        return if (trygdeavgiftsgrunnlag == null) null else
            GrunnlagsOpplysningerDto(
                mapTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag),
                AvgiftDto(
                    trygdeavgiftsperioder = trygdeavgiftsperioder.map {
                        TrygdeavgiftsperiodeDto(
                            fom = it.fom,
                            tom = it.tom,
                            inntektskildetype = it.grunnlagInntekstperiode.type,
                            inntektPerMd = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi.intValueExact(),
                            arbeidsgiversavgiftBetales = it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                            avgiftssats = it.trygdesats.toDouble(),
                            avgiftPerMd = it.trygdeavgiftsbeløpMd.verdi.intValueExact()
                        )
                    },
                    totalInntekt = trygdeavgiftTotalBeregner.hentTotalInntekt(trygdeavgiftsperioder),
                    totalAvgift = trygdeavgiftTotalBeregner.hentTotalAvgift(trygdeavgiftsperioder) ?: BigDecimal.ZERO
                )
            )
    }


    @PutMapping("/{avregningID}")
    fun hentAvregning(@PathVariable("avregningID") avregningID: Long, @RequestBody årsavregningRequest: ÅrsavregningRequest): ResponseEntity<Unit> {
        // TODO bruk årsavregningService

        return ResponseEntity.noContent().build()
    }
}

data class ÅrsavregningResponse(
    val aar: Int,
    val tidligereGrunnlagsopplysninger: GrunnlagsOpplysningerDto?,
    val avvikFunnet: Boolean?,
    val nyttGrunnlag: GrunnlagsOpplysningerDto?,
    val endeligAvgift: AvgiftDto?,
    val avregning: AvregningDto?
)

data class ÅrsavregningRequest(
    val aar: Int,
    val tidligereFakturertBeloep: Int?,
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektskperioder: List<InntektsperiodeDto>,
)

data class GrunnlagsOpplysningerDto(
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    val avgift: AvgiftDto
)

data class TrygdeavgiftsgrunnlagDto(
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskperioder: List<InntekskildeDto>,
)

data class AvgiftDto(
    val trygdeavgiftsperioder: List<TrygdeavgiftsperiodeDto>,
    val totalInntekt: BigDecimal,
    val totalAvgift: BigDecimal
)

data class Skatteforholdsperiode(
    val fom: LocalDate, val tom: LocalDate, val skatteplikttype: Skatteplikttype
)

data class Medlemskapsperiode(
    val fom: LocalDate, val tom: LocalDate, val trygdedekning: Trygdedekninger
)

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektskildetype: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val inntektPerMd: Int,
    val avgiftssats: Double,
    val avgiftPerMd: Int
)

data class InntektsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val inntektPerMd: Int
)

data class AvregningDto(
    val nyttTotalbeloep: BigDecimal?,
    val tidligereFakturertBeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)
