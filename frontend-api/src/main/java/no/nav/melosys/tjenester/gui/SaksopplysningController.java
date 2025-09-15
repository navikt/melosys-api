package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksopplysninger")
@Tag(name = "saksopplysninger")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningController {

    private final OppfriskSaksopplysningerService oppfriskSaksopplysningerService;
    private final Aksesskontroll aksesskontroll;
    private final BehandlingService behandlingService;

    public SaksopplysningController(OppfriskSaksopplysningerService oppfriskSaksopplysningerService, Aksesskontroll aksesskontroll, BehandlingService behandlingService) {
        this.oppfriskSaksopplysningerService = oppfriskSaksopplysningerService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingService = behandlingService;
    }

    @GetMapping("oppfriskning/{behandlingID}")
    @Operation(summary = "Oppfrisker saksopplysing basert på behandlingsid", description = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "404", description = "Behandling ikke funnet"),
        @ApiResponse(responseCode = "500", description = "Uventet teknisk Feil")
    })
    public ResponseEntity<Void> oppfriskSaksopplysning(@PathVariable("behandlingID") long behandlingID, @RequestParam(required = false) boolean inkluderSiste5Aar) {
        aksesskontroll.autoriserSkriv(behandlingID);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erEøsPensjonist()) {
            oppfriskSaksopplysningerService.oppdaterRegisteropplysningerForEøsPensjonist(behandlingID, inkluderSiste5Aar);
        } else {
            oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(behandlingID, inkluderSiste5Aar);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("oppfriskning/aarsavregning/{behandlingID}")
    @Operation(summary = "Oppfrisker saksopplysinger basert på behandlingsid, sletter ikke medlemskapsperioder.", description = ("Oppdater saksopplysinger basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "404", description = "Behandling ikke funnet"),
        @ApiResponse(responseCode = "500", description = "Uventet teknisk Feil")
    })
    public ResponseEntity<Void> oppdaterSaksopplysninger(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriserSkriv(behandlingID);
        oppfriskSaksopplysningerService.oppdaterSaksopplysningerForAarsavregning(behandlingID);
        return ResponseEntity.noContent().build();
    }
}
