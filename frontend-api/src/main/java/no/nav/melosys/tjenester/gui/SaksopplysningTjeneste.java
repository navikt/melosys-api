package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.SaksopplysningerService;
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
    public ResponseEntity oppfriskSaksopplysning(@PathVariable("behandlingID") long id) throws MelosysException {
        saksopplysningerService.oppfriskSaksopplysning(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // FIXME: oppfriskning/{behandlingID}/status fjernes fra JSON-schema
}
