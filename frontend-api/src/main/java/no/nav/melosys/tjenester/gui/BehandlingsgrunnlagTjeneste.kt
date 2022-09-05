package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.PeriodeOgLandPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = "behandlingsgrunnlag")
@RequestMapping("/behandlingsgrunnlag")
public class BehandlingsgrunnlagTjeneste {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final Aksesskontroll aksesskontroll;

    public BehandlingsgrunnlagTjeneste(BehandlingsgrunnlagService behandlingsgrunnlagService,
                                       Aksesskontroll aksesskontroll) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity<BehandlingsgrunnlagGetDto> hentBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") long behandlingID
    ) {
        aksesskontroll.autoriser(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag));
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity<BehandlingsgrunnlagGetDto> oppdaterBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto
    ) {

        aksesskontroll.autoriserSkriv(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagPostDto.getData());
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag));
    }

    @PostMapping("/{behandlingID}/periodeOgLand")
    public ResponseEntity<Void> oppdaterBehandlingsgrunnlagPeriodeOgLand(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody PeriodeOgLandPostDto periodeOgLandPostDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlagPeriodeOgLand(behandlingID,
            new Periode(periodeOgLandPostDto.fom(), periodeOgLandPostDto.tom()),
            new Soeknadsland(periodeOgLandPostDto.land(), false));
        return ResponseEntity.noContent().build();
    }
}
