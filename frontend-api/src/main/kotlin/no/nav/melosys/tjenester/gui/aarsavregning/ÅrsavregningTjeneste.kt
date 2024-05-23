package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.sak.ÅrsavregningService
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
        // TODO val årsavregnig = årsavregningService.hentÅrsavregnig(avregningID)
        return ResponseEntity.ok(
            ÅrsavregningResponse(
                aar = 2023,
                forskuddsvisFakturertAvgift = FakturertAvgift(
                    skatteforholdsperioder = listOf(
                        Skatteforholdsperiode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Skatteplikttype.SKATTEPLIKTIG),
                        Skatteforholdsperiode(LocalDate.of(2023, 8, 1), LocalDate.of(2023, 12, 31), Skatteplikttype.IKKE_SKATTEPLIKTIG)
                    ),
                    medlemskapsperioder = listOf(
                        Medlemskapsperiode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON),
                        Medlemskapsperiode(
                            LocalDate.of(2023, 8, 1),
                            LocalDate.of(2023, 12, 31),
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        )
                    ),
                    trygdeavgiftsperioder = listOf(
                        Trygdeavgiftsperiode(
                            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, true, 40000, 0.0, 0
                        ), Trygdeavgiftsperiode(
                            LocalDate.of(2023, 8, 1), LocalDate.of(2023, 12, 31), Inntektskildetype.INNTEKT_FRA_UTLANDET, false, 15000, 42.2, 6330
                        )
                    ),
                    totalInntektPerMd = 690000,
                    totalAvgiftPerMd = 127020
                ),
                avvikFunnet = false,
                endeligAvgift = EndeligAvgift(
                    skatteforholdsperioder = listOf(
                        Skatteforholdsperiode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Skatteplikttype.SKATTEPLIKTIG),
                        Skatteforholdsperiode(LocalDate.of(2023, 8, 1), LocalDate.of(2023, 12, 31), Skatteplikttype.IKKE_SKATTEPLIKTIG)
                    ),
                    inntektskperioder = listOf(
                        Inntektsperiode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, true, 95000)
                    ),
                    trygdeavgiftsperioder = listOf(
                        Trygdeavgiftsperiode(
                            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 31), Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, true, 40000, 0.0, 0
                        ), Trygdeavgiftsperiode(
                            LocalDate.of(2023, 8, 1), LocalDate.of(2023, 12, 31), Inntektskildetype.INNTEKT_FRA_UTLANDET, false, 15000, 42.2, 6330
                        )
                    ),
                    totalInntektPerMd = 690000,
                    totalAvgiftPerMd = 127020

                ),
                avregning = Avregning(
                    nyttTotalbeloep = 24280,
                    tidligereFakturertBeloep = 21170,
                    tilFaktureringBeloep = 3110
                )
            )
        )
    }
}

@PutMapping("/{avregningID}")
fun hentAvregning(@PathVariable("avregningID") avregningID: Long, @RequestBody årsavregningRequest: ÅrsavregningRequest): ResponseEntity<Unit> {
    // TODO val årsavregnig = årsavregningService.oppdaterÅrsavregning(avregningID)
    return ResponseEntity.noContent().build()
}

data class ÅrsavregningResponse(
    val aar: Int,
    val forskuddsvisFakturertAvgift: FakturertAvgift,
    val avvikFunnet: Boolean?,
    val endeligAvgift: EndeligAvgift?,
    val avregning: Avregning?
)

data class ÅrsavregningRequest(
    val aar: Int,
    val tidligereFakturertBeloep: Int?,
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektskperioder: List<Inntektsperiode>,
)

data class FakturertAvgift(
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val medlemskapsperioder: List<Medlemskapsperiode>,
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

data class EndeligAvgift(
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektskperioder: List<Inntektsperiode>,
    val trygdeavgiftsperioder: List<Trygdeavgiftsperiode>,
    val totalInntektPerMd: Int,
    val totalAvgiftPerMd: Int
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
