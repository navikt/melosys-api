package no.nav.melosys.tjenester.gui.avklartefakta;

import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsgrunnlagRequest;
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
                                          @RequestBody OppdaterTrygdeavgiftsgrunnlagRequest oppdaterTrygdeavgiftsgrunnlagRequest
    ) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(behandlingID, oppdaterTrygdeavgiftsgrunnlagRequest);
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
