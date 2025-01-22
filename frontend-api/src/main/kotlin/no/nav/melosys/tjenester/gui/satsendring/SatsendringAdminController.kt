package no.nav.melosys.tjenester.gui.satsendring

import no.nav.melosys.service.AdminController
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
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

        val finnBehandlingerMedSatsendringer = satsendringFinner.finnBehandlingerMedSatsendring(aar)
        val satsendringRapportDto = SatsendringRapportDto(
            finnBehandlingerMedSatsendringer.år,
            behandlingerMedSatsendring = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendring.map { it ->
                    BehandlingForSatstendringDto(
                        it.behandlingID,
                        it.saksnummer
                    )
                },
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendring.size
            ),
            behandlingerMedSatsendringOgNyVurdering = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendringOgNyVurdering.map { it ->
                    BehandlingForSatstendringDto(
                        it.behandlingID,
                        it.saksnummer
                    )
                },
                finnBehandlingerMedSatsendringer.behandlingerMedSatsendringOgNyVurdering.size
            ),
            behandlingerUtenSatsendring = BehandlingerMedTotalDto(
                finnBehandlingerMedSatsendringer.behandlingerUtenSatsendring.map { it ->
                    BehandlingForSatstendringDto(
                        it.behandlingID,
                        it.saksnummer
                    )
                },
                finnBehandlingerMedSatsendringer.behandlingerUtenSatsendring.size
            )
        )
        return ResponseEntity.ok(satsendringRapportDto)
    }

    override fun getApiKey(): String {
        return apiKey
    }


}

data class SatsendringRapportDto(
    val år: Int,
    val behandlingerMedSatsendring: BehandlingerMedTotalDto,
    val behandlingerMedSatsendringOgNyVurdering: BehandlingerMedTotalDto,
    val behandlingerUtenSatsendring: BehandlingerMedTotalDto
)

data class BehandlingerMedTotalDto(
    val behandlinger: List<BehandlingForSatstendringDto>,
    val total: Int
)

data class BehandlingForSatstendringDto(
    val behandlingID: Long,
    val saksnummer: String,
)
