package no.nav.melosys.tjenester.gui.satsendring

import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/satsendringer")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class SatsendringController(private val satsendringFinner: SatsendringFinner) {


    @GetMapping("/{aar}/rapport")
    fun lagRapport(@PathVariable("aar") aar: Int): ResponseEntity<SatsendringRapportDto> {
        val finnBehandlingerMedSatsendringer = satsendringFinner.finnBehandlingerMedSatsendring(aar)
        val satsendringRapportDto = SatsendringRapportDto(
            finnBehandlingerMedSatsendringer.år,
            behandlingerMedSatsendring = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendring,
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendring.size
            ),
            behandlingerMedSatsendringOgNyVurdering = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendringOgNyVurdering,
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendringOgNyVurdering.size
            ),
            behandlingerUtenSatsendring = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerUtenSatsendring,
                finnBehandlingerMedSatsendringer.behandlingerUtenSatsendring.size
            )
        )
        return ResponseEntity.ok(satsendringRapportDto)
    }


}

data class SatsendringRapportDto(
    val år: Int,
    val behandlingerMedSatsendring: BehandlingerMedTotalDto,
    val behandlingerMedSatsendringOgNyVurdering: BehandlingerMedTotalDto,
    val behandlingerUtenSatsendring: BehandlingerMedTotalDto
)

data class BehandlingerMedTotalDto(
    val behandlinger: List<BehandlingForSatstendring>,
    val total: Number
)
