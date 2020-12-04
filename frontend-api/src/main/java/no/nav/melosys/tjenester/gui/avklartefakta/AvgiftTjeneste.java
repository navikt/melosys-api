package no.nav.melosys.tjenester.gui.avklartefakta;

import no.nav.melosys.domain.avgift.OppdaterAvgiftsgrunnlagRequest;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avgift.AvgiftsgrunnlagService;
import org.springframework.web.bind.annotation.*;

@RestController
public class AvgiftTjeneste {

    private final AvgiftsgrunnlagService avgiftsgrunnlagService;
    private final TilgangService tilgangService;

    public AvgiftTjeneste(AvgiftsgrunnlagService avgiftsgrunnlagService, TilgangService tilgangService) {
        this.avgiftsgrunnlagService = avgiftsgrunnlagService;
        this.tilgangService = tilgangService;
    }

    @PutMapping("/behandlinger/{behandlingID}/avgift/grunnlag")
    public Object oppdaterAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                          @RequestBody OppdaterAvgiftsgrunnlagRequest oppdaterAvgiftsgrunnlagRequest
    ) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        avgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingID, oppdaterAvgiftsgrunnlagRequest);
        return null;
    }

    @GetMapping("/behandlinger/{behandlingID}/avgift/grunnlag")
    public Object hentAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        return null;
    }

    @PutMapping("/behandlinger/{behandlingID}/avgift/beregning")
    public Object oppdaterBeregningsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                         @RequestBody Object oppdaterBeregningsgrunnlagRequest) {

        return null;
    }

    @GetMapping("/behandlinger/{behandlingID}/avgift/beregning")
    public Object hentBeregningsresultat(@PathVariable("behandlingID") long behandlingID) {
        return null;
    }
}
