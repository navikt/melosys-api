package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.service.OppfriskSaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksopplysninger")
@Api(tags = {"saksopplysninger"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningTjeneste {

    private final OppfriskSaksopplysningerService oppfriskSaksopplysningerService;
    private final Aksesskontroll aksesskontroll;

    public SaksopplysningTjeneste(OppfriskSaksopplysningerService oppfriskSaksopplysningerService, Aksesskontroll aksesskontroll) {
        this.oppfriskSaksopplysningerService = oppfriskSaksopplysningerService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("oppfriskning/{behandlingID}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public ResponseEntity<Void> oppfriskSaksopplysning(@PathVariable("behandlingID") long behandlingID, @RequestParam(required = false) boolean medFamilierelasjoner) {
        aksesskontroll.autoriserSkriv(behandlingID);
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandlingID, medFamilierelasjoner);
        return ResponseEntity.noContent().build();
    }
}
