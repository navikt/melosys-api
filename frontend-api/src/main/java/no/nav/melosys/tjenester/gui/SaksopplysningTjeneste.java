package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SaksopplysningerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "saksopplysninger" })
@RestController("/saksopplysninger")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningTjeneste extends RestTjeneste {

    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public SaksopplysningTjeneste(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    @GetMapping("oppfriskning/{behandlingID}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public ResponseEntity oppfriskSaksopplysning(@PathVariable("behandlingID") @ApiParam("behandlingsid.") long id) throws IkkeFunnetException, TekniskException {
        saksopplysningerService.oppfriskSaksopplysning(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("oppfriskning/{behandlingID}/status")
    @ApiOperation(value = "Status på oppfrisking av behandling ", notes = ("Returnerer status (Progress/Done) på oppfrisking av behandling"))
    public ResponseEntity hentOppfriskingStatusForBehandling(@ApiParam @PathVariable("behandlingID") long behandlingID) {
        String status;
        if (saksopplysningerService.harAktivOppfrisking(behandlingID)) {
            status = "\"PROGRESS\"";
        } else {
            status = "\"DONE\"";
        }
        return ResponseEntity.ok(status);
    }
}
