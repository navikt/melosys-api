package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/utpeking")
@Api(tags = {"saksflyt", "utpeking"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvvisUtpekingTjeneste {

    private final UtpekingService utpekingService;
    private final Aksesskontroll aksesskontroll;


    public AvvisUtpekingTjeneste(UtpekingService utpekingService, Aksesskontroll aksesskontroll) {
        this.utpekingService = utpekingService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping("{behandlingID}/avvis")
    public ResponseEntity<Void> avvisUtpeking(@PathVariable("behandlingID") Long behandlingId,
                                              @RequestBody UtpekingAvvisDto utpekingAvvisDto) {
        aksesskontroll.autoriser(behandlingId);
        utpekingService.avvisUtpeking(behandlingId, utpekingAvvisDto.tilDomene());
        return ResponseEntity.noContent().build();
    }
}
