package no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift

import io.swagger.annotations.Api
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@Api(tags = ["trygdeavgift"])
@RequestMapping("/behandlinger/{behandlingID}/trygdeavgift")
class TrygdeavgiftController(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val aksesskontroll: Aksesskontroll
) {

    @GetMapping("/mottaker")
    fun hentTrygdeavgiftMottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftMottakerDto> {
        aksesskontroll.autoriser(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftMottakerDto(trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingID))
        )
    }

    @PutMapping("/beregning")
    fun beregnTrygdeavgiftsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody trygdeavgiftsgrunnlagDto: TrygdeavgiftsgrunnlagDto
    ): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)

        val skatteforholdsperioder = trygdeavgiftsgrunnlagDto.skatteforholdsperioder.tilSkatteforholdsPerioder()
        val inntektsperioder = trygdeavgiftsgrunnlagDto.inntektskilder.tilInntektsPerioder()

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            skatteforholdsperioder,
            inntektsperioder
        )

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/beregning")
    fun hentBeregnetTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(behandlingID)

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/grunnlag/opprinnelig")
    fun hentOpprinneligTrygdeavgiftsgrunnlagDersomDetEksisterer(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftsgrunnlagDto> {
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperioder = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftsgrunnlagDto(trygdeavgiftsperioder)
        )
    }

    @GetMapping("/fakturamottaker")
    fun hentFakturamottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<FakturamottakerDto> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(FakturamottakerDto(trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)))
    }

    @DeleteMapping
    fun slettTrygdeavgiftsperioder(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Unit> {
        aksesskontroll.autoriser(behandlingID)
        trygdeavgiftService.slettTrygdeavgiftsperioderPåBehandlingsresultat(behandlingID)
        return ResponseEntity.noContent().build()
    }

    private fun List<SkatteforholdTilNorgeDto>.tilSkatteforholdsPerioder() = map {
        SkatteforholdTilNorge().apply {
            fomDato = it.fomDato
            tomDato = it.tomDato
            skatteplikttype = it.skatteplikttype
        }
    }

    private fun List<InntektskildeDto>.tilInntektsPerioder() = map {
        Inntektsperiode().apply {
            fomDato = it.fomDato
            tomDato = it.tomDato
            type = it.type
            isArbeidsgiversavgiftBetalesTilSkatt = it.arbeidsgiversavgiftBetales

            if (it.erMaanedsbelop) {
                avgiftspliktigMndInntekt = Penger(it.avgiftspliktigInntekt)
            } else {
                avgiftspliktigTotalinntekt = Penger(it.avgiftspliktigInntekt)
            }
        }
    }
}
