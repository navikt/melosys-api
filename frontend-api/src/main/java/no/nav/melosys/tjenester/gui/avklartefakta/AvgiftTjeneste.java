package no.nav.melosys.tjenester.gui.avklartefakta;

import no.nav.melosys.domain.avgift.OppdaterAvgiftsgrunnlagRequest;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/behandlinger/{behandlingID}/avgift")
public class AvgiftTjeneste {

    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final TilgangService tilgangService;

    public AvgiftTjeneste(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService, TilgangService tilgangService) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.tilgangService = tilgangService;
    }

    @PutMapping("/grunnlag")
    public Object oppdaterAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                          @RequestBody OppdaterAvgiftsgrunnlagRequest oppdaterAvgiftsgrunnlagRequest
    ) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingID, oppdaterAvgiftsgrunnlagRequest);
    }

    @GetMapping("/grunnlag")
    public Object hentAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingID);
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
