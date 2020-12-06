package no.nav.melosys.tjenester.gui.avklartefakta;

import no.nav.melosys.domain.avgift.OppdaterAvgiftsgrunnlagRequest;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avgift.AvgiftsgrunnlagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/behandlinger/{behandlingID}/avgift")
public class AvgiftTjeneste {

    private final AvgiftsgrunnlagService avgiftsgrunnlagService;
    private final TilgangService tilgangService;

    public AvgiftTjeneste(AvgiftsgrunnlagService avgiftsgrunnlagService, TilgangService tilgangService) {
        this.avgiftsgrunnlagService = avgiftsgrunnlagService;
        this.tilgangService = tilgangService;
    }

    @PutMapping("/grunnlag")
    public Object oppdaterAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                          @RequestBody OppdaterAvgiftsgrunnlagRequest oppdaterAvgiftsgrunnlagRequest
    ) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return avgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingID, oppdaterAvgiftsgrunnlagRequest);
    }

    @GetMapping("/grunnlag")
    public Object hentAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return avgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingID);
    }

    @PutMapping("/beregning")
    public Object oppdaterBeregningsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                         @RequestBody Object oppdaterBeregningsgrunnlagRequest) {
        return null;
    }

    @GetMapping("/beregning")
    public Object hentBeregningsresultat(@PathVariable("behandlingID") long behandlingID) {
        return null;
    }
}
