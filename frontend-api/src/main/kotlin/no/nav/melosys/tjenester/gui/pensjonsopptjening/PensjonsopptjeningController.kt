package no.nav.melosys.tjenester.gui.pensjonsopptjening

import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.popp.PensjonsopptjeningOppslag
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "pensjonsopptjening")
@RequestMapping("/behandlinger/{behandlingID}/pensjonsopptjening")
class PensjonsopptjeningController(
    private val pensjonsopptjeningOppslag: PensjonsopptjeningOppslag,
    private val aksesskontroll: Aksesskontroll,
    private val unleash: Unleash,
) {

    @GetMapping
    fun hentPensjonsopptjening(
        @PathVariable("behandlingID") behandlingID: Long,
    ): ResponseEntity<PensjonsopptjeningResponse> {
        aksesskontroll.autoriser(behandlingID)

        if (!unleash.isEnabled(ToggleName.MELOSYS_VIS_PENSJONSOPPTJENING_POPP)) {
            return ResponseEntity.noContent().build()
        }

        val pensjonsopptjening = pensjonsopptjeningOppslag.hent(behandlingID)

        return ResponseEntity.ok(
            PensjonsopptjeningResponse(
                inntektsAr = pensjonsopptjening.inntektsAr,
                perioder = pensjonsopptjening.perioder.map {
                    PensjonsopptjeningPeriodeDto(aar = it.aar, pgi = it.pgi, kilde = it.kilde)
                },
            )
        )
    }
}

data class PensjonsopptjeningResponse(
    val inntektsAr: Int,
    val perioder: List<PensjonsopptjeningPeriodeDto>,
)

data class PensjonsopptjeningPeriodeDto(
    val aar: Int,
    val pgi: Long,
    val kilde: String,
)
