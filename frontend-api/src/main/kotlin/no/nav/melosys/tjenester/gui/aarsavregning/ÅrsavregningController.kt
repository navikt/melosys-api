package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.AarsavregningBehandlingsvalg
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

    //TODO - MELOSYS-7267: Kvitt oss med denne metoden, og lage endepunkt for hver endring som kan gjøres
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
            årsavregningOppdaterRequest.avregning.nyttTotalbeloep,
            årsavregningOppdaterRequest.avregning.tidligereFakturertBeloepAvgiftssystem,
            avgift25Prosent = årsavregningOppdaterRequest.avregning.avgift25Prosent
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PostMapping("/grunnlagstype")
    fun oppdaterHarDeltGrunnlag(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody harDeltGrunnlagRequest: HarDeltGrunnlagRequest
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdaterHarDeltGrunnlag(
            behandlingID,
            harDeltGrunnlagRequest.harDeltGrunnlag
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PutMapping("/{aarsavregningID}/behandlingsvalg/{behandlingsvalg}")
    fun oppdaterBehandlingsvalg(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("aarsavregningID") aarsavregningID: Long,
        @PathVariable("behandlingsvalg") behandlingsvalg: AarsavregningBehandlingsvalg
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val årsavregning = årsavregningService.oppdater(
            behandlingID,
            aarsavregningID,
            null,
            null,
            behandlingsvalg = behandlingsvalg
        )

        return ResponseEntity.ok(
            lagÅrsavregningResponse(årsavregning)
        )
    }

    @PutMapping("/{aarsavregningID}/harAvvik/{harAvvik}")
    fun oppdaterHarAvvik(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("aarsavregningID") aarsavregningID: Long,
        @PathVariable("harAvvik") harAvvik: Boolean
    ): ResponseEntity<ÅrsavregningResponse> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val behandlingsvalg = if (harAvvik) {
            AarsavregningBehandlingsvalg.OPPLYSNINGER_ENDRET
        } else {
            AarsavregningBehandlingsvalg.OPPLYSNINGER_UENDRET
        }

        val årsavregning = årsavregningService.oppdater(
            behandlingID,
            aarsavregningID,
            null,
            null,
            behandlingsvalg = behandlingsvalg
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
            harAvvik = årsavregningModel.harAvvik,
            nyttGrunnlag = hentGrunnlagsopplysninger(årsavregningModel.nyttGrunnlag, årsavregningModel.endeligAvgift),
            endeligAvgift = null,
            avregning = AvregningDto(
                nyttTotalbeloep = årsavregningModel.nyttTotalbeloep,
                tidligereFakturertBeloep = årsavregningModel.tidligereFakturertBeloep,
                tilFaktureringBeloep = årsavregningModel.tilFaktureringBeloep,
                tidligereFakturertBeloepAvgiftssystem = årsavregningModel.tidligereFakturertBeloepAvgiftssystem,
                avgift25Prosent = årsavregningModel.avgift25Prosent,
            ),
            harDeltGrunnlag = årsavregningModel.harDeltGrunnlag,
            behandlingsvalg = årsavregningModel.behandlingsvalg?.name
        )

    private fun hentGrunnlagsopplysninger(
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag?,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): GrunnlagsOpplysningerDto? {
        return if (trygdeavgiftsgrunnlag == null) null else
            GrunnlagsOpplysningerDto(
                mapTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag),
                AvgiftDto(
                    trygdeavgiftsperioder = trygdeavgiftsperioder
                        .map {
                            val avgiftspliktigMndInntekt = it.grunnlagInntekstperiode?.kalkulertMndInntekt(verdiAvrundet = true) ?: BigDecimal.ZERO

                            TrygdeavgiftsperiodeDto(
                                fom = it.fom,
                                tom = it.tom,
                                inntektskildetype = it.grunnlagInntekstperiode?.type,
                                inntektPerMd = avgiftspliktigMndInntekt,
                                arbeidsgiversavgiftBetales = it.grunnlagInntekstperiode?.isArbeidsgiversavgiftBetalesTilSkatt,
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

data class HarDeltGrunnlagRequest(
    val harDeltGrunnlag: Boolean
)

data class ÅrsavregningResponse(
    val aarsavregningID: Long,
    val aar: Int,
    val tidligereGrunnlagsopplysninger: GrunnlagsOpplysningerDto?,
    val harAvvik: Boolean?, // Beholdt for bakoverkompatibilitet
    val nyttGrunnlag: GrunnlagsOpplysningerDto?,
    val endeligAvgift: AvgiftDto?,
    val avregning: AvregningDto?,
    val harDeltGrunnlag: Boolean?,
    val behandlingsvalg: String?
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
    val inntektskildetype: Inntektskildetype?,
    val arbeidsgiversavgiftBetales: Boolean?,
    val inntektPerMd: BigDecimal,
    val avgiftssats: Double,
    val avgiftPerMd: Int
)

data class AvregningDto(
    val nyttTotalbeloep: BigDecimal?,
    val tidligereFakturertBeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
    val tidligereFakturertBeloepAvgiftssystem: BigDecimal?,
    val avgift25Prosent: BigDecimal?,
)
