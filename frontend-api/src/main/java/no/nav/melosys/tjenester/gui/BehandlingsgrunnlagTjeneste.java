package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = "behandlingsgrunnlag")
@RequestMapping("/behandlingsgrunnlag")
public class BehandlingsgrunnlagTjeneste {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public BehandlingsgrunnlagTjeneste(BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity hentBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID) throws IkkeFunnetException {
        return ResponseEntity.ok(
            new BehandlingsgrunnlagGetDto(behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID))
        );
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity oppdaterBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID,
                                                      @RequestBody BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto) throws IkkeFunnetException {
        return ResponseEntity.ok(
            new BehandlingsgrunnlagGetDto(behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagPostDto.getData()))
        );
    }
}
