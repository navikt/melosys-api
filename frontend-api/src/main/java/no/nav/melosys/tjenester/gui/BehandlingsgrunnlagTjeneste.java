package no.nav.melosys.tjenester.gui;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsgrunnlagDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@RequestMapping("/behandlingsgrunnlag")
public class BehandlingsgrunnlagTjeneste {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public BehandlingsgrunnlagTjeneste(BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity hentBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID) throws IkkeFunnetException {
        return ResponseEntity.ok(
            new BehandlingsgrunnlagDto(behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID))
        );
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity oppdaterBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID,
                                                      BehandlingsgrunnlagDto behandlingsgrunnlagDto) throws IkkeFunnetException {
        return ResponseEntity.ok(
            new BehandlingsgrunnlagDto(behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagDto.getBehandlingsgrunnlagData()))
        );
    }
}
