package no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.service.avgift.EøsPensjonistTrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

val log = mu.KotlinLogging.logger {}

@Protected
@RestController
@Tag(name = "trygdeavgift")
@RequestMapping("/behandlinger/{behandlingID}/trygdeavgift")
class TrygdeavgiftController(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val eøsPensjonistTrygdeavgiftsBeregningService: EøsPensjonistTrygdeavgiftsberegningService,
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
    fun hentBeregnetTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Any> {
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(behandlingID)

        try {
            return ResponseEntity.ok(
                BeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
            )
        } catch (e: IllegalStateException) {
            if (e.message == "avgiftspliktigMndInntekt og avgiftspliktigTotalinntekt er null") {
                log.error("Avgiftspliktig inntekt er null for behandlingID: $behandlingID", e)
                return ResponseEntity.badRequest().body("Avgiftspliktig inntekt er null for behandlingID: $behandlingID")
            }
            throw e
        }
    }

    @GetMapping("/eos-pensjonist/beregning")
    fun hentBeregnetTrygdeavgiftForPensjonist(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Any> {
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.hentTrygdeavgiftsberegningForEosPensjonist(behandlingID)

        try {
            return ResponseEntity.ok(
                EøsPensjonistBeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
            )
        } catch (e: IllegalStateException) {
            if (e.message == "avgiftspliktigMndInntekt og avgiftspliktigTotalinntekt er null") {
                log.error("Avgiftspliktig inntekt er null for behandlingID: $behandlingID", e)
                return ResponseEntity.badRequest().body("Avgiftspliktig inntekt er null for behandlingID: $behandlingID")
            }
            throw e
        }
    }

    @PutMapping("/eos-pensjonist/beregning")
    fun eøsPensjonistBeregnTrygdeavgiftsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody trygdeavgiftsgrunnlagDto: TrygdeavgiftsgrunnlagDto
    ): ResponseEntity<EøsPensjonistBeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)

        val skatteforholdsperioder = trygdeavgiftsgrunnlagDto.skatteforholdsperioder.tilSkatteforholdsPerioder()
        val inntektsperioder = trygdeavgiftsgrunnlagDto.inntektskilder.tilInntektsPerioder()

        val trygdeavgiftsperiodeSet = eøsPensjonistTrygdeavgiftsBeregningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            skatteforholdsperioder,
            inntektsperioder
        )

        return ResponseEntity.ok(
            EøsPensjonistBeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/grunnlag/opprinnelig")
    fun hentOpprinneligTrygdeavgiftsgrunnlagDersomDetEksisterer(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftsgrunnlagDto> {
        aksesskontroll.autoriser(behandlingID)

        val grunnlagModel = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftsgrunnlagDto(grunnlagModel)
        )
    }

    @GetMapping("/fakturamottaker")
    fun hentFakturamottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<FakturamottakerDto> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(FakturamottakerDto(trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)))
    }

    @GetMapping
    fun hentTrygdeavgiftsperioder(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<List<TrygdeavgiftsperiodeDto>> {
        aksesskontroll.autoriser(behandlingID)
        val trygdeavgiftsperioder = trygdeavgiftService.hentTrygdeavgiftsperioderPåBehandlingsresultat(behandlingID)
        return ResponseEntity.ok(trygdeavgiftsperioder.map { TrygdeavgiftsperiodeDto(it) })
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
                avgiftspliktigMndInntekt = Penger(it.avgiftspliktigInntekt ?: BigDecimal.ZERO)
            } else {
                avgiftspliktigTotalinntekt = Penger(it.avgiftspliktigInntekt ?: BigDecimal.ZERO)
            }
        }
    }
}
