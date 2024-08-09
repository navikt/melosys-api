package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Protected
@RestController
@Api(tags = ["årsavregning", "trygdeavgift"])
@RequestMapping("/aarsavregninger")
class ÅrsavregningController(
    private val årsavregningService: ÅrsavregningService,
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

    @PutMapping("/{behandlingID}/ferdigstill")
    fun ferdigstillÅrsavregning(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Void> {
        årsavregningService.ferdigstillÅrsavregning(behandlingID)

        return ResponseEntity.noContent().build()
    }

    data class LagÅrsavregningRequest(
        val aar: Int
    )

    private fun lagÅrsavregningResponse(årsavregningModel: ÅrsavregningModel) =
        ÅrsavregningResponse(
            aar = årsavregningModel.år,
            tidligereGrunnlagsopplysninger = hentTidligereGrunnlagsopplysninger(årsavregningModel),
            avvikFunnet = årsavregningModel.nyttGrunnlag != null,
            nyttGrunnlag = null,
            endeligAvgift = null,
            avregning = Avregning(
                nyttTotalbeloep = årsavregningModel.nyttTotalbeloep?.intValueExact() ?: 0,
                tidligereFakturertBeloep = årsavregningModel.tidligereFakturertBeloep?.intValueExact() ?: 0,
                tilFaktureringBeloep = årsavregningModel.tilFaktureringBeloep?.intValueExact() ?: 0,
            )
        )


    private fun hentTidligereGrunnlagsopplysninger(årsavregningModel: ÅrsavregningModel): TidligereGrunnlagsopplysninger? {
        return if (årsavregningModel.tidligereGrunnlag == null) null else
            TidligereGrunnlagsopplysninger(
                Trygdeavgiftsgrunnlag(
                    medlemskapsperioder = årsavregningModel.tidligereGrunnlag?.medlemskapsperioder?.map {
                        Medlemskapsperiode(
                            it.fom,
                            it.tom,
                            it.dekning
                        )
                    }
                        .orEmpty(),
                    skatteforholdsperioder = årsavregningModel.tidligereGrunnlag?.skatteforholdsperioder?.map {
                        Skatteforholdsperiode(
                            it.fom,
                            it.tom,
                            it.skatteplikttype
                        )
                    }.orEmpty(),
                    inntektskperioder = årsavregningModel.tidligereGrunnlag?.innteksperioder?.map {
                        Inntektsperiode(
                            it.fom,
                            it.tom,
                            it.type,
                            it.isArbeidsgiversavgiftBetalesTilSkatt,
                            it.avgiftspliktigInntektMnd.verdi.intValueExact()
                        )
                    }.orEmpty(),
                ),
                Avgift(
                    trygdeavgiftsperioder = årsavregningModel.tidligereAvgift.map {
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
                    totalInntekt = hentTotalInntekt(årsavregningModel.tidligereAvgift),
                    totalAvgift = hentTotalAvgift(årsavregningModel.tidligereAvgift)
                )
            )
    }

    private fun hentTotalInntekt(trygdeavgiftsperioder: List<no.nav.melosys.domain.avgift.Trygdeavgiftsperiode>): Int {
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                beskrivelse = "FIXME"
            )
        }
        return årsavregningService.beregnTotalbeløpForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder)).intValueExact()
    }

    private fun hentTotalAvgift(trygdeavgiftsperioder: List<no.nav.melosys.domain.avgift.Trygdeavgiftsperiode>): Int {
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.trygdeavgiftsbeløpMd.verdi,
                beskrivelse = "FIXME"
            )
        }
        return årsavregningService.beregnTotalbeløpForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder)).intValueExact()
    }
}

@PutMapping("/{avregningID}")
fun hentAvregning(@PathVariable("avregningID") avregningID: Long, @RequestBody årsavregningRequest: ÅrsavregningRequest): ResponseEntity<Unit> {
    // TODO bruk årsavregningService

    return ResponseEntity.noContent().build()
}

data class ÅrsavregningResponse(
    val aar: Int,
    val tidligereGrunnlagsopplysninger: TidligereGrunnlagsopplysninger?,
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

data class TidligereGrunnlagsopplysninger(
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
    val totalInntekt: Int,
    val totalAvgift: Int
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
