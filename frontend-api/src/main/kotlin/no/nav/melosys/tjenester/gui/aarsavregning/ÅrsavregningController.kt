package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner.kalkulertMndInntekt
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.InntektskildeDto
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
@RequestMapping("/behandlinger/{behandlingID}/aarsavregninger")
class ÅrsavregningController(
    private val årsavregningService: ÅrsavregningService,
    private val aksesskontroll: Aksesskontroll,
) {
    @GetMapping
    fun hentÅrsavregning(
        @PathVariable("behandlingID") behandlingID: Long
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriser(behandlingID)

        val årsavregning = årsavregningService.finnÅrsavregningForBehandling(behandlingID) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PostMapping
    fun opprettNyÅrsavregning(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody årsavregningRequest: LagÅrsavregningRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.opprettÅrsavregning(behandlingID, årsavregningRequest.aar)

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PutMapping("/{aarsavregningID}")
    fun oppdaterÅrsavregning(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("aarsavregningID") aarsavregningID: Long,
        @RequestBody årsavregningOppdaterRequest: ÅrsavregningOppdaterRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdater(
            behandlingID,
            aarsavregningID,
            årsavregningOppdaterRequest.avregning.tidligereFakturertBeloep,
            årsavregningOppdaterRequest.avregning.nyttTotalbeloep
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    private fun lagÅrsavregningResponse(årsavregningModel: ÅrsavregningModel) =
        ÅrsavregningResponse(
            aarsavregningID = årsavregningModel.årsavregningID,
            aar = årsavregningModel.år,
            tidligereGrunnlagsopplysninger = hentGrunnlagsopplysninger(årsavregningModel.tidligereGrunnlag, årsavregningModel.tidligereAvgift),
            avvikFunnet = årsavregningModel.tilFaktureringBeloep != BigDecimal.ZERO,
            nyttGrunnlag = hentGrunnlagsopplysninger(årsavregningModel.nyttGrunnlag, årsavregningModel.endeligAvgift),
            endeligAvgift = null,
            avregning = AvregningDto(
                nyttTotalbeloep = årsavregningModel.nyttTotalbeloep,
                tidligereFakturertBeloep = årsavregningModel.tidligereFakturertBeloep,
                tilFaktureringBeloep = årsavregningModel.tilFaktureringBeloep,
            )
        )

    private fun hentGrunnlagsopplysninger(
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): GrunnlagsOpplysningerDto? {
        return if (trygdeavgiftsgrunnlag == null) null else
            GrunnlagsOpplysningerDto(
                mapTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag),
                AvgiftDto(
                    trygdeavgiftsperioder = trygdeavgiftsperioder.filter { it.grunnlagInntekstperiode != null }
                        .map {
                            val avgiftspliktigMndInntekt = it.grunnlagInntekstperiode!!.kalkulertMndInntekt(verdiAvrundet = true)

                            TrygdeavgiftsperiodeDto(
                                fom = it.fom,
                                tom = it.tom,
                                inntektskildetype = it.grunnlagInntekstperiode!!.type,
                                inntektPerMd = avgiftspliktigMndInntekt,
                                arbeidsgiversavgiftBetales = it.grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt,
                                avgiftssats = it.trygdesats.toDouble(),
                                avgiftPerMd = it.trygdeavgiftsbeløpMd.verdi.intValueExact()
                            )
                        },
                    totalInntekt = TotalbeløpBeregner.hentTotalinntekt(trygdeavgiftsperioder),
                    totalAvgift = TotalbeløpBeregner.hentTotalavgift(trygdeavgiftsperioder) ?: BigDecimal.ZERO
                )
            )
    }

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
                val avgiftspliktigInntekt = if (it.erMaanedsbelop) {
                    it.avgiftspliktigInntekt?.verdi
                } else {
                    it.avgiftspliktigTotalInntekt?.verdi
                }

                InntektskildeDto(
                    it.type,
                    it.isArbeidsgiversavgiftBetalesTilSkatt,
                    avgiftspliktigInntekt,
                    it.fom,
                    it.tom,
                    it.erMaanedsbelop
                )
            }.orEmpty()
        )
}

data class LagÅrsavregningRequest(
    val aar: Int,
)

data class ÅrsavregningResponse(
    val aarsavregningID: Long,
    val aar: Int,
    val tidligereGrunnlagsopplysninger: GrunnlagsOpplysningerDto?,
    val avvikFunnet: Boolean?,
    val nyttGrunnlag: GrunnlagsOpplysningerDto?,
    val endeligAvgift: AvgiftDto?,
    val avregning: AvregningDto?
)

data class ÅrsavregningOppdaterRequest(
    val avregning: AvregningDto
)

data class GrunnlagsOpplysningerDto(
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    val avgift: AvgiftDto
)

data class TrygdeavgiftsgrunnlagDto(
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskperioder: List<InntektskildeDto>,
)

data class AvgiftDto(
    val trygdeavgiftsperioder: List<TrygdeavgiftsperiodeDto>,
    val totalInntekt: BigDecimal,
    val totalAvgift: BigDecimal
)

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektskildetype: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val inntektPerMd: BigDecimal,
    val avgiftssats: Double,
    val avgiftPerMd: Int
)

data class AvregningDto(
    val nyttTotalbeloep: BigDecimal?,
    val tidligereFakturertBeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)
