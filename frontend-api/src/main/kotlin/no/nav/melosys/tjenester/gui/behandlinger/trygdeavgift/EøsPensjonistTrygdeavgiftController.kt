package no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.service.avgift.EøsPensjonistTrygdeavgiftsBeregningService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal


@Protected
@RestController
@Tag(name = "trygdeavgift")
@RequestMapping("/behandlinger/{behandlingID}/eos-pensjonist/trygdeavgift")
class EøsPensjonistTrygdeavgiftController(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val eøsPensjonistTrygdeavgiftsBeregningService: EøsPensjonistTrygdeavgiftsBeregningService,
    private val aksesskontroll: Aksesskontroll,
) {

    @GetMapping("/mottaker")
    fun hentTrygdeavgiftMottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftMottakerDto> {
        log.info("Henter trygdeavgift mottaker for EØS pensjonist")
        aksesskontroll.autoriser(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftMottakerDto(trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingID))
        )
    }

    @PutMapping("/beregning")
    fun beregnTrygdeavgiftsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody trygdeavgiftsgrunnlagDto: TrygdeavgiftsgrunnlagDto
    ): ResponseEntity<EøsPensjonistBeregnetTrygdeavgiftDto> {
        log.info("Beregner trygdeavgift for EØS pensjonist")
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)

        val skatteforholdsperioder = trygdeavgiftsgrunnlagDto.skatteforholdsperioder.tilSkatteforholdsPerioder()
        val inntektsperioder = trygdeavgiftsgrunnlagDto.inntektskilder.tilInntektsPerioder()

        val trygdeavgiftsperiodeSet = eøsPensjonistTrygdeavgiftsBeregningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            skatteforholdsperioder,
            inntektsperioder
        )
        log.warn("Ferdig med beregning, returnerer ")

        return ResponseEntity.ok(
            EøsPensjonistBeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/beregning")
    fun hentBeregnetTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Any> {
        log.info("Henter eksisterende beregnet trygdeavgift for EØS pensjonist")
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

    @GetMapping("/grunnlag/opprinnelig")
    fun hentOpprinneligTrygdeavgiftsgrunnlagDersomDetEksisterer(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftsgrunnlagDto> {
        log.info("Henter opprinnelig trygdeavgiftsgrunnlag for EØS pensjonist")
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperioder = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftsgrunnlagDto(trygdeavgiftsperioder)
        )
    }

    @GetMapping("/fakturamottaker")
    fun hentFakturamottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<FakturamottakerDto> {
        log.info("Henter fakturamottaker for EØS pensjonist")
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(FakturamottakerDto(trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)))
    }

    @DeleteMapping
    fun slettTrygdeavgiftsperioder(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Unit> {
        log.info("Sletter trygdeavgiftsperioder for EØS pensjonist")
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
