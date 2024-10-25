package no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift

import io.swagger.annotations.Api
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.FakturamottakerDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.InntektskildeDto.Companion.tilInntektsPeriodeDtos
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.SkatteforholdTilNorgeDto.Companion.tilSkatteforholdsPeriodeDtos
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftMottakerDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto
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
        val skatteforholdsperioderTemp = trygdeavgiftsgrunnlagDto.skatteforholdsperioder.tilSkatteforholdsPeriodeDtos()
        val inntektsperioderTemp = trygdeavgiftsgrunnlagDto.inntektskilder.tilInntektsPeriodeDtos()

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            trygdeavgiftsgrunnlagDto.tilRequest(),
            skatteforholdsperioderTemp,
            inntektsperioderTemp
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
}
