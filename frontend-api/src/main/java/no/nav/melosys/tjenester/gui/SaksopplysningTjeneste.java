package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksopplysninger")
@Api(tags = { "saksopplysninger" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningTjeneste {

    private final SaksopplysningerService saksopplysningerService;
    private final TilgangService tilgangService;

    @Autowired
    public SaksopplysningTjeneste(SaksopplysningerService saksopplysningerService, TilgangService tilgangService) {
        this.saksopplysningerService = saksopplysningerService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("oppfriskning/{behandlingID}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public ResponseEntity oppfriskSaksopplysning(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        saksopplysningerService.oppfriskSaksopplysning(behandlingID);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("oppfriskning/{behandlingID}/status")
    @ApiOperation(value = "Status på oppfrisking av behandling ", notes = ("Returnerer status (Progress/Done) på oppfrisking av behandling"))
    public ResponseEntity hentOppfriskingStatusForBehandling(@PathVariable("behandlingID") long behandlingID) {
        String status;
        if (saksopplysningerService.harAktivOppfrisking(behandlingID)) {
            status = "\"PROGRESS\"";
        } else {
            status = "\"DONE\"";
        }
        return ResponseEntity.ok(status);
    }
}
