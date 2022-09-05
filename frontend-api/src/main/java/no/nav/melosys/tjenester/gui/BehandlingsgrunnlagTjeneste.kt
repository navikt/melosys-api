package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.PeriodeOgLandPostDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@Api(tags = ["behandlingsgrunnlag"])
@RequestMapping("/behandlingsgrunnlag")
class BehandlingsgrunnlagTjeneste(private val behandlingsgrunnlagService: BehandlingsgrunnlagService,
                                  private val aksesskontroll: Aksesskontroll) {
    @GetMapping("/{behandlingID}")
    fun hentBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") behandlingID: Long): ResponseEntity<BehandlingsgrunnlagGetDto> {
        aksesskontroll.autoriser(behandlingID)
        val behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID)
        return ResponseEntity.ok(BehandlingsgrunnlagGetDto(behandlingsgrunnlag))
    }

    @PostMapping("/{behandlingID}")
    fun oppdaterBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") behandlingID: Long,
        @RequestBody behandlingsgrunnlagPostDto: BehandlingsgrunnlagPostDto): ResponseEntity<BehandlingsgrunnlagGetDto> {
        aksesskontroll.autoriserSkriv(behandlingID)
        val behandlingsgrunnlag = behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagPostDto.data)
        return ResponseEntity.ok(BehandlingsgrunnlagGetDto(behandlingsgrunnlag))
    }

    @PostMapping("/{behandlingID}/periodeOgLand")
    fun oppdaterBehandlingsgrunnlagPeriodeOgLand(
        @PathVariable(value = "behandlingID") behandlingID: Long,
        @RequestBody periodeOgLandPostDto: PeriodeOgLandPostDto): ResponseEntity<Void> {
        aksesskontroll.autoriserSkriv(behandlingID)
        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlagPeriodeOgLand(behandlingID,
            Periode(periodeOgLandPostDto.fom(), periodeOgLandPostDto.tom()),
            Soeknadsland(periodeOgLandPostDto.land(), false))
        return ResponseEntity.noContent().build()
    }
}
