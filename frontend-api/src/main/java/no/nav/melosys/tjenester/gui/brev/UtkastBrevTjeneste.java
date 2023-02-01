package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.brev.OppdaterUtkastComponent;
import no.nav.melosys.service.brev.UtkastBrevService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingRequest;
import no.nav.melosys.tjenester.gui.dto.brev.HentUtkastResponse;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Protected
@RestController
@RequestMapping("/brev/utkast")
@Api(tags = {"brev", "utkast"})
public class UtkastBrevTjeneste {

    private final UtkastBrevService utkastBrevService;
    private final Aksesskontroll aksesskontroll;

    public UtkastBrevTjeneste(UtkastBrevService utkastBrevService,
                              Aksesskontroll aksesskontroll) {
        this.utkastBrevService = utkastBrevService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle brevutkast for en behandling", response = HentUtkastResponse.class, responseContainer = "List")
    public List<HentUtkastResponse> hentUtkast(@PathVariable long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return utkastBrevService.hentUtkast(behandlingID)
            .stream()
            .map(HentUtkastResponse::av)
            .toList();
    }

    @PostMapping(value = "/{behandlingID}")
    @ApiOperation(value = "Lagrer et brevutkast på en behandling")
    public ResponseEntity<Void> lagreUtkast(@PathVariable long behandlingID,
                                            @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        String saksbehandlerID = SubjectHandler.getInstance().getUserID();

        utkastBrevService.lagreUtkast(behandlingID, saksbehandlerID, brevbestillingRequest.tilUtkast());

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{behandlingID}/{utkastBrevID}")
    @ApiOperation(value = "Oppdaterer et eksisterende utkast")
    public ResponseEntity<Void> oppdaterUtkast(@PathVariable long behandlingID,
                                               @PathVariable long utkastBrevID,
                                               @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        String saksbehandlerID = SubjectHandler.getInstance().getUserID();

        utkastBrevService.oppdaterUtkast(new OppdaterUtkastComponent.RequestDto(utkastBrevID, behandlingID, saksbehandlerID, brevbestillingRequest.tilUtkast()));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{behandlingID}/{utkastBrevID}")
    @ApiOperation(value = "Sletter et brevutkast")
    public ResponseEntity<Void> slettUtkast(@PathVariable long behandlingID, @PathVariable long utkastBrevID) {
        aksesskontroll.autoriser(behandlingID);

        utkastBrevService.slettUtkast(utkastBrevID);

        return ResponseEntity.noContent().build();
    }
}
