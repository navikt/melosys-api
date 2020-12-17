package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.OppfriskSaksopplysningerService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksopplysninger")
@Api(tags = { "saksopplysninger" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningTjeneste {

    private final OppfriskSaksopplysningerService oppfriskSaksopplysningerService;
    private final TilgangService tilgangService;

    @Autowired
    public SaksopplysningTjeneste(OppfriskSaksopplysningerService oppfriskSaksopplysningerService, TilgangService tilgangService) {
        this.oppfriskSaksopplysningerService = oppfriskSaksopplysningerService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("oppfriskning/{behandlingID}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public ResponseEntity<Void> oppfriskSaksopplysning(@PathVariable("behandlingID") long behandlingID, @RequestParam(required = false) boolean medFamilierelasjoner) throws MelosysException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandlingID, medFamilierelasjoner);
        return ResponseEntity.noContent().build();
    }
}
