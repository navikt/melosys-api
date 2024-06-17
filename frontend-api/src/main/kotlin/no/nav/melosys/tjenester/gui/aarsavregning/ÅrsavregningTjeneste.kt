package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.service.avgift.aarsavregning.Årsavregning
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
                tidligereGrunnlagsopplysninger = hentTidligereGrunnlagsopplysninger(årsavregning),
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

    @PostMapping("/opprettAarsavregning")
    fun opprettNyÅrsavregning(@RequestBody årsavregningRequest: LagÅrsavregningRequest): ResponseEntity<Long> {
        return ResponseEntity.ok(årsavregningService.opprettNyÅrsavregning(årsavregningRequest.behandlingsId, årsavregningRequest.aar))
    }

    data class LagÅrsavregningRequest(
        val aar: Int,
        val behandlingsId: Long
    )

    private fun hentTidligereGrunnlagsopplysninger(årsavregning: Årsavregning): TidligereGrunnlagsopplysninger? {
        return if (årsavregning.tidligereGrunnlag == null) null else
            TidligereGrunnlagsopplysninger(
                Trygdeavgiftsgrunnlag(
                    medlemskapsperioder = årsavregning.tidligereGrunnlag?.medlemskapsperioder?.map { Medlemskapsperiode(it.fom, it.tom, it.dekning) }
                        .orEmpty(),
                    skatteforholdsperioder = årsavregning.tidligereGrunnlag?.skatteforholdsperioder?.map {
                        Skatteforholdsperiode(
                            it.fom,
                            it.tom,
                            it.skatteplikttype
                        )
                    }.orEmpty(),
                    inntektskperioder = årsavregning.tidligereGrunnlag?.innteksperioder?.map {
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
                    totalInntekt = hentTotalInntekt(årsavregning.tidligereAvgift),
                    totalAvgift = hentTotalAvgift(årsavregning.tidligereAvgift)
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
