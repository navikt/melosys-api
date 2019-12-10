package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/vedtak")
@Api(tags = {"saksflyt", "vedtak"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VedtakTjeneste {
    private final VedtakService vedtakService;
    private final TilgangService tilgangService;

    @Autowired
    public VedtakTjeneste(VedtakService vedtakService, TilgangService tilgangService) {
        this.vedtakService = vedtakService;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/fatt")
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public ResponseEntity fattVedtak(@PathVariable("behandlingID") long behandlingID, @RequestBody FattVedtakDto fattVedtakDto) throws MelosysException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null) {
            throw new FunksjonellException("");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), fattVedtakDto.getFritekst(), fattVedtakDto.getMottakerinstitusjon());
        return ResponseEntity.ok().build();
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public ResponseEntity endreVedtak(@PathVariable("behandlingID") long behandlingID, @RequestBody EndreVedtakDto endreVedtakDto) throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new FunksjonellException("Mangler BegrunnelseKode");
        }
        if (endreVedtakDto.getBehandlingstype() == null) {
            throw new FunksjonellException("Mangler Behandlingstype");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getBehandlingstype(), endreVedtakDto.getFritekst());
        return ResponseEntity.ok().build();
    }
}