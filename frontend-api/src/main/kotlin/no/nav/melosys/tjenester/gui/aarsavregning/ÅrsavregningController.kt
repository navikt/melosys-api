package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
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
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.FastsettingsperiodeDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@Protected
@RestController
@Tags(
    Tag(name = "årsavregning"),
    Tag(name = "trygdeavgift")
)
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

    //TODO - MELOSYS-7267: Kvitt oss med denne  metoden, og lage endepunkt for hver endring som kan gjøres
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
            årsavregningOppdaterRequest.avregning.beregnetAvgiftBelop,
            årsavregningOppdaterRequest.avregning.trygdeavgiftFraAvgiftssystemet,
            manueltAvgiftBeloep = årsavregningOppdaterRequest.avregning.manueltAvgiftBeloep
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PostMapping("/grunnlagstype")
    fun oppdaterHarTrygdeavgiftFraAvgiftssystemet(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody harTrygdeavgiftFraAvgiftssystemetRequest: HarTrygdeavgiftFraAvgiftssystemetRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(
            behandlingID,
            harTrygdeavgiftFraAvgiftssystemetRequest.harTrygdeavgiftFraAvgiftssystemet
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PostMapping("/skjoennsfastsatt")
    fun oppdaterHarSkjoennsfastsattInntektsgrunnlag(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody harSkjoennsfastsattInntekt: HarSkjoennsfastsattInntektRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdaterHarSkjoennsfastsattInntektsgrunnlag(
            behandlingID,
            harSkjoennsfastsattInntekt.harSkjoennsfastsattInntekt
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PutMapping("/{aarsavregningID}/endeligAvgift/{endeligAvgift}")
    fun oppdaterEndeligAvgift(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("aarsavregningID") aarsavregningID: Long,
        @PathVariable("endeligAvgift") endeligAvgift: EndeligAvgiftValg
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdater(
            behandlingID,
            aarsavregningID,
            null,
            null,
            endeligAvgift = endeligAvgift
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    private fun lagÅrsavregningResponse(årsavregningModel: ÅrsavregningModel) =
        ÅrsavregningResponse(
            aarsavregningID = årsavregningModel.årsavregningID,
            aar = årsavregningModel.år,
            tidligereGrunnlagsopplysninger = hentTidligereGrunnlagsopplysninger(
                årsavregningModel
            ),
            nyttGrunnlag = hentGrunnlagsopplysninger(årsavregningModel.nyttGrunnlag, årsavregningModel.endeligAvgift),
            endeligAvgift = null,
            avregning = AvregningDto(
                beregnetAvgiftBelop = årsavregningModel.beregnetAvgiftBelop,
                tidligereFakturertBeloep = årsavregningModel.tidligereFakturertBeloep,
                tilFaktureringBeloep = årsavregningModel.tilFaktureringBeloep,
                trygdeavgiftFraAvgiftssystemet = årsavregningModel.trygdeavgiftFraAvgiftssystemet,
                manueltAvgiftBeloep = årsavregningModel.manueltAvgiftBeloep,
            ),
            harTrygdeavgiftFraAvgiftssystemet = årsavregningModel.harTrygdeavgiftFraAvgiftssystemet,
            endeligAvgiftValg = årsavregningModel.endeligAvgiftValg?.name,
            harSkjoennsfastsattInntekt = årsavregningModel.harSkjoennsfastsattInntektsgrunnlag
        )

    private fun hentGrunnlagsopplysninger(
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): GrunnlagsOpplysningerDto? {
        return trygdeavgiftsgrunnlag?.let { grunnlag ->
            GrunnlagsOpplysningerDto(
                trygdeavgiftsgrunnlag = mapTrygdeavgiftsgrunnlag(grunnlag),
                avgift = AvgiftDto(
                    trygdeavgiftsperioder = mapTilTrygdeavgiftperiodeDto(trygdeavgiftsperioder),
                    totalInntekt = TotalbeløpBeregner.hentTotalinntekt(trygdeavgiftsperioder),
                    totalAvgift = TotalbeløpBeregner.hentTotalavgift(trygdeavgiftsperioder) ?: BigDecimal.ZERO
                )
            )
        }
    }

    private fun hentTidligereGrunnlagsopplysninger(
        årsavregningModel: ÅrsavregningModel
    ): TidligereGrunnlagsOpplysningerDto? {
        return årsavregningModel.tidligereGrunnlag?.let { grunnlag ->
            TidligereGrunnlagsOpplysningerDto(
                trygdeavgiftsgrunnlag = mapTrygdeavgiftsgrunnlag(grunnlag),
                avgift = AvgiftDto(
                    trygdeavgiftsperioder = mapTilTrygdeavgiftperiodeDto(årsavregningModel.tidligereAvgift),
                    totalInntekt = TotalbeløpBeregner.hentTotalinntekt(årsavregningModel.tidligereAvgift),
                    totalAvgift = TotalbeløpBeregner.hentTotalavgift(årsavregningModel.tidligereAvgift) ?: BigDecimal.ZERO
                ),
                tidligereTrygdeavgiftFraAvgiftssystemet = årsavregningModel.tidligereTrygdeavgiftFraAvgiftssystemet,
                tidligereÅrsavregningManueltAvgiftBeloep = årsavregningModel.tidligereÅrsavregningmanueltAvgiftBeloep
            )
        }
    }

    private fun mapTilTrygdeavgiftperiodeDto(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) =
        trygdeavgiftsperioder.map { periode ->
            val avgiftspliktigMndInntekt = periode.grunnlagInntekstperiode?.kalkulertMndInntekt(verdiAvrundet = true) ?: BigDecimal.ZERO

            TrygdeavgiftsperiodeDto(
                fom = periode.fom,
                tom = periode.tom,
                inntektskildetype = periode.grunnlagInntekstperiode?.type,
                inntektPerMd = avgiftspliktigMndInntekt,
                arbeidsgiversavgiftBetales = periode.grunnlagInntekstperiode?.isArbeidsgiversavgiftBetalesTilSkatt,
                avgiftssats = periode.trygdesats.toDouble(),
                avgiftPerMd = periode.trygdeavgiftsbeløpMd.verdi.intValueExact()
            )
        }

    private fun mapTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?) =
        TrygdeavgiftsgrunnlagDto(
            fastsettingsperioder = trygdeavgiftsgrunnlag?.fastsettingsperioder?.map {
                FastsettingsperiodeDto(
                    0,
                    it.fom,
                    it.tom,
                    it.bestemmelse,
                    InnvilgelsesResultat.INNVILGET,
                    it.dekning,
                    it.medlemskapstype
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

data class HarTrygdeavgiftFraAvgiftssystemetRequest(
    val harTrygdeavgiftFraAvgiftssystemet: Boolean
)

data class HarSkjoennsfastsattInntektRequest(
    val harSkjoennsfastsattInntekt: Boolean
)

data class ÅrsavregningResponse(
    val aarsavregningID: Long,
    val aar: Int,
    val tidligereGrunnlagsopplysninger: TidligereGrunnlagsOpplysningerDto?,
    val nyttGrunnlag: GrunnlagsOpplysningerDto?,
    val endeligAvgift: AvgiftDto?,
    val avregning: AvregningDto?,
    val harTrygdeavgiftFraAvgiftssystemet: Boolean?,
    val endeligAvgiftValg: String?,
    val harSkjoennsfastsattInntekt: Boolean?,
)

data class ÅrsavregningOppdaterRequest(
    val avregning: AvregningDto
)

data class TidligereGrunnlagsOpplysningerDto(
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    val avgift: AvgiftDto,
    val tidligereTrygdeavgiftFraAvgiftssystemet: BigDecimal?,
    val tidligereÅrsavregningManueltAvgiftBeloep: BigDecimal?,
)

data class GrunnlagsOpplysningerDto(
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    val avgift: AvgiftDto,
)

data class TrygdeavgiftsgrunnlagDto(
    val fastsettingsperioder: List<FastsettingsperiodeDto>,
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
    val inntektskildetype: Inntektskildetype?,
    val arbeidsgiversavgiftBetales: Boolean?,
    val inntektPerMd: BigDecimal,
    val avgiftssats: Double,
    val avgiftPerMd: Int
)

data class AvregningDto(
    val beregnetAvgiftBelop: BigDecimal?,
    val tidligereFakturertBeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
    val trygdeavgiftFraAvgiftssystemet: BigDecimal?,
    val manueltAvgiftBeloep: BigDecimal?,
)
