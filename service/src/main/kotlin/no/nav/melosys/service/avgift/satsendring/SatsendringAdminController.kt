package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/admin/satsendringer")
class SatsendringAdminController(
    private val satsendringFinner: SatsendringFinner,
    private val behandlingService: BehandlingService,
    private val prosessinstansService: ProsessinstansService
) {

    @GetMapping("/{aar}/rapport")
    fun lagRapport(
        @PathVariable("aar") aar: Int
    ): ResponseEntity<SatsendringRapportDto> {
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

    @PostMapping("/{aar}/fagsaker/{saksnummer}")
    fun opprettSatsendring(
        @PathVariable("aar") aar: Int,
        @PathVariable("saksnummer") saksnummer: String
    ): ResponseEntity<String> {
        val finnBehandlingerMedSatsendring = satsendringFinner.finnBehandlingerMedSatsendring(aar)

        finnBehandlingerMedSatsendring.behandlingerMedSatsendring
            .find { it.saksnummer == saksnummer }
            ?.let {
                val behandling = behandlingService.hentBehandling(it.behandlingID)
                val uuid = prosessinstansService.opprettSatsendringBehandlingFor(behandling, aar)
                return ResponseEntity.ok("Oppretter satsendring prosessinstans: $uuid for sak $saksnummer og behandlingID: ${it.behandlingID}")
            }

        finnBehandlingerMedSatsendring.behandlingerMedSatsendringOgNyVurdering
            .find { it.saksnummer == saksnummer }
            ?.let {
                val behandling = behandlingService.hentBehandling(it.behandlingID)
                val uuid = prosessinstansService.opprettSatsendringBehandlingNyVurderingFor(behandling, aar)
                return ResponseEntity.ok("Oppretter satsendring ny vurdering prosessinstans: $uuid for sak $saksnummer og behandlingID: ${it.behandlingID}")
            }

        throw IkkeFunnetException("Sak: $saksnummer ikke funnet i satsendringsrapporten")
    }

    private fun behandlingerMedTotalDto(behandlinger: List<BehandlingForSatstendring>) =
        BehandlingerMedTotalDto(
            behandlinger.map {
                BehandlingForSatstendringDto(
                    it.behandlingID,
                    it.saksnummer,
                    it.feilÅrsak
                )
            },
            behandlinger.size
        )
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
