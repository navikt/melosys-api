package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.service.AdminController
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/admin/satsendringer")
class SatsendringAdminController(
    private val satsendringFinner: SatsendringFinner,
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : AdminController {

    @GetMapping("/{aar}/rapport")
    fun lagRapport(
        @PathVariable("aar") aar: Int,
        @RequestHeader(AdminController.API_KEY_HEADER) apiKey: String?
    ): ResponseEntity<SatsendringRapportDto> {
        validerApikey(apiKey)

        val avgiftSatsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(aar)
        val satsendringRapportDto = SatsendringRapportDto(
            avgiftSatsendringInfo.år,
            behandlingerMedSatsendring = behandlingerMedTotalDto(avgiftSatsendringInfo.behandlingerMedSatsendring),
            behandlingerMedSatsendringOgNyVurdering = behandlingerMedTotalDto(avgiftSatsendringInfo.behandlingerMedSatsendringOgNyVurdering),
            behandlingerUtenSatsendring = behandlingerMedTotalDto(avgiftSatsendringInfo.behandlingerUtenSatsendring),
            behandlingerSomFeilet = behandlingerMedTotalDto(avgiftSatsendringInfo.behandlingerSomFeilet)
        )
        return ResponseEntity.ok(satsendringRapportDto)
    }

    private fun behandlingerMedTotalDto(behandlinger: List<BehandlingForSatstendring>) =
        BehandlingerMedTotalDto(
            behandlinger.map {
                BehandlingForSatstendringDto(
                    it.behandlingID,
                    it.saksnummer,
                    it.feilAarsak
                )
            },
            behandlinger.size
        )

    override fun getApiKey(): String = apiKey
}

data class SatsendringRapportDto(
    val år: Int,
    val behandlingerMedSatsendring: BehandlingerMedTotalDto,
    val behandlingerMedSatsendringOgNyVurdering: BehandlingerMedTotalDto,
    val behandlingerUtenSatsendring: BehandlingerMedTotalDto,
    val behandlingerSomFeilet: BehandlingerMedTotalDto
)

data class BehandlingerMedTotalDto(
    val behandlinger: List<BehandlingForSatstendringDto>,
    val total: Int
)

data class BehandlingForSatstendringDto(
    val behandlingID: Long,
    val saksnummer: String,
    val feilAarsak: String? = null
)
