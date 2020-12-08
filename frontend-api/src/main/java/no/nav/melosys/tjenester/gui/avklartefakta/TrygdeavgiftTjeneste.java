package no.nav.melosys.tjenester.gui.avklartefakta;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.AvgiftsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterAvgiftsgrunnlagDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/behandlinger/{behandlingID}/trygdeavgift")
public class TrygdeavgiftTjeneste {

    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    private final TilgangService tilgangService;

    public TrygdeavgiftTjeneste(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                TrygdeavgiftsberegningService trygdeavgiftsberegningService,
                                TilgangService tilgangService) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.trygdeavgiftsberegningService = trygdeavgiftsberegningService;
        this.tilgangService = tilgangService;
    }

    @PutMapping("/grunnlag")
    public AvgiftsgrunnlagDto oppdaterAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                      @RequestBody OppdaterAvgiftsgrunnlagDto oppdaterAvgiftsgrunnlagDto
    ) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return AvgiftsgrunnlagDto.av(
            trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(
                behandlingID, oppdaterAvgiftsgrunnlagDto.til()
            )
        );
    }

    @GetMapping("/grunnlag")
    public AvgiftsgrunnlagDto hentAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return AvgiftsgrunnlagDto.av(
            trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingID)
        );
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
