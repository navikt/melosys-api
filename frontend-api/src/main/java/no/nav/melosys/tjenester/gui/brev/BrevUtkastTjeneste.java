package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.service.brev.UtkastBrevService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.brev.HentUtkastResponse;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Protected
@RestController
@RequestMapping("/brev/utkast")
@Api(tags = {"brev", "utkast"})
@RequestScope
public class BrevUtkastTjeneste {

    private final UtkastBrevService brevUtkastService;
    private final Aksesskontroll aksesskontroll;

    public BrevUtkastTjeneste(UtkastBrevService brevUtkastService,
                              Aksesskontroll aksesskontroll) {
        this.brevUtkastService = brevUtkastService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle brevutkast for en behandling", response = BrevbestillingDto.class, responseContainer = "List")
    public List<HentUtkastResponse> hentUtkast(@PathVariable long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return brevUtkastService.hentUtkast(behandlingID)
            .stream()
            .map(HentUtkastResponse::av)
            .toList();
    }

    @PostMapping(value = "/{behandlingID}")
    @ApiOperation(value = "Lagrer et brevutkast på en behandling")
    public void hentTilgjengeligeMottakere(@PathVariable long behandlingID,
                                           @RequestBody BrevbestillingDto brevbestillingDto) {
        String saksbehandlerID = SubjectHandler.getInstance().getUserID();
        aksesskontroll.autoriser(behandlingID);

        brevUtkastService.lagreUtkast(behandlingID, saksbehandlerID, brevbestillingDto.tilUtkast());
    }
}
