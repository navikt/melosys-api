package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Protected
@RestController
@Api(tags = ["årsavregning", "trygdeavgift"])
@RequestMapping("/aarsavregninger")
class ÅrsavregningTjeneste(
    private val årsavregningService: ÅrsavregningService,
) {
    @GetMapping("/{avregningID}")
    fun hentAvregning(@PathVariable("avregningID") avregningID: Long): ResponseEntity<ÅrsavregningResponse> {
        val årsavregning = årsavregningService.hentÅrsavregning(avregningID)
        return ResponseEntity.ok(
            ÅrsavregningResponse(
                aar = årsavregning.år,
                tidligereOpplysninger = TidligereOpplysninger(
                    Trygdeavgiftsgrunnlag(
                        medlemskapsperioder = årsavregning.tidligereGrunnlag?.medlemskapsperioder?.map { Medlemskapsperiode(it.fom, it.tom, it.dekning) }.orEmpty(),
                        skatteforholdsperioder = årsavregning.tidligereGrunnlag?.skatteforholdsperioder?.map { Skatteforholdsperiode(it.fom, it.tom, it.skatteplikttype) }.orEmpty(),
                        inntektskperioder = årsavregning.tidligereGrunnlag?.innteksperioder?.map { Inntektsperiode(it.fom, it.tom, it.type, it.isArbeidsgiversavgiftBetalesTilSkatt, it.avgiftspliktigInntektMnd.verdi.intValueExact()) }.orEmpty(),
                    ),
                    Avgift(
                        trygdeavgiftsperioder = årsavregning.tidligereAvgift.map {
                            Trygdeavgiftsperiode(
                                fom = it.fom,
                                tom = it.tom,
                                inntektskildetype = it.grunnlagInntekstperiode.type,
                                inntektPerMd = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi.intValueExact(),
                                arbeidsgiversavgiftBetales = it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                                avgiftssats = it.trygdesats.toDouble(),
                                avgiftPerMd = it.trygdeavgiftsbeløpMd.verdi.intValueExact()
                            )
                        },
                        totalInntektPerMd = årsavregning.tidligereGrunnlag?.innteksperioder?.sumOf { it.avgiftspliktigInntektMnd.verdi.intValueExact() } ?: 0,
                        totalAvgiftPerMd = årsavregning.tidligereAvgift?.sumOf { it.trygdeavgiftsbeløpMd.verdi.intValueExact() } ?:0
                    )
                ),
                avvikFunnet = årsavregning.nyttGrunnlag != null,
                nyttGrunnlag = null,
                endeligAvgift = null,
                avregning = Avregning(
                    nyttTotalbeloep = årsavregning?.nyttTotalbeloep?.intValueExact() ?: 0,
                    tidligereFakturertBeloep = årsavregning?.tidligereFakturertBeloep?.intValueExact() ?: 0,
                    tilFaktureringBeloep = årsavregning?.tilFaktureringBeloep?.intValueExact() ?: 0,
                )
            )
        )
    }
}

@PutMapping("/{avregningID}")
fun hentAvregning(@PathVariable("avregningID") avregningID: Long, @RequestBody årsavregningRequest: ÅrsavregningRequest): ResponseEntity<Unit> {
    // TODO bruk årsavregningService
    return ResponseEntity.noContent().build()
}

data class ÅrsavregningResponse(
    val aar: Int,
    val tidligereOpplysninger: TidligereOpplysninger?,
    val avvikFunnet: Boolean?,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: Avgift?,
    val avregning: Avregning?
)

data class ÅrsavregningRequest(
    val aar: Int,
    val tidligereFakturertBeloep: Int?,
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektskperioder: List<Inntektsperiode>,
)

data class TidligereOpplysninger(
    val trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
    val avgift: Avgift
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<Medlemskapsperiode>,
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektskperioder: List<Inntektsperiode>,
)

data class Avgift(
    val trygdeavgiftsperioder: List<Trygdeavgiftsperiode>,
    val totalInntektPerMd: Int,
    val totalAvgiftPerMd: Int
)

data class Skatteforholdsperiode(
    val fom: LocalDate, val tom: LocalDate, val skatteplikttype: Skatteplikttype
)

data class Medlemskapsperiode(
    val fom: LocalDate, val tom: LocalDate, val trygdedekning: Trygdedekninger
)

data class Trygdeavgiftsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektskildetype: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val inntektPerMd: Int,
    val avgiftssats: Double,
    val avgiftPerMd: Int
)

data class Inntektsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val inntektPerMd: Int
)

data class Avregning(
    val nyttTotalbeloep: Int,
    val tidligereFakturertBeloep: Int,
    val tilFaktureringBeloep: Int,
)
