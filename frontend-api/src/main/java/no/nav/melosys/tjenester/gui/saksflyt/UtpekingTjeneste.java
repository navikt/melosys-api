package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/utpeking")
@Api(tags = {"saksflyt", "utpeking"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtpekingTjeneste {

    private final UtpekingService utpekingService;
    private final TilgangService tilgangService;


    @Autowired
    public UtpekingTjeneste(UtpekingService utpekingService, TilgangService tilgangService) {
        this.utpekingService = utpekingService;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/avvis")
    public ResponseEntity avvisUtpeking(@PathVariable("behandlingID") Long behandlingId, @RequestBody UtpekingAvvisDto utpekingAvvisDto) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingId);
        utpekingService.avvisUtpeking(behandlingId, utpekingAvvisDto.tilDomene());
        return ResponseEntity.noContent().build();
    }
}
