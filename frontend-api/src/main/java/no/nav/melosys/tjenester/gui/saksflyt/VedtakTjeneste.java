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
import no.nav.melosys.tjenester.gui.dto.RevurderingOpprettetDto;
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
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null || fattVedtakDto.getVedtakstype() == null) {
            throw new FunksjonellException("BehandlingsresultatTypeKode eller vedtakstype mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), fattVedtakDto.getFritekst(),
            fattVedtakDto.getMottakerinstitusjon(), fattVedtakDto.getVedtakstype(), fattVedtakDto.getRevurderBegrunnelse());
        return ResponseEntity.ok().build();
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public ResponseEntity endreVedtak(@PathVariable("behandlingID") long behandlingID, @RequestBody EndreVedtakDto endreVedtakDto)
        throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new FunksjonellException("BegrunnelseKode mangler.");
        }
        if (endreVedtakDto.getBehandlingstype() == null) {
            throw new FunksjonellException("Behandlingstype mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getBehandlingstype(), endreVedtakDto.getFritekst());
        return ResponseEntity.ok().build();
    }

    @PostMapping("{behandlingID}/revurder")
    @ApiOperation(value = "Korrigerer eller omgjør vedtak for en sak ved å opprette en ny behandling basert på en eksisterende")
    public ResponseEntity revurderVedtak(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);

        long nyBehandlingID = vedtakService.revurderVedtak(behandlingID);
        return ResponseEntity.ok(new RevurderingOpprettetDto(nyBehandlingID));
    }
}