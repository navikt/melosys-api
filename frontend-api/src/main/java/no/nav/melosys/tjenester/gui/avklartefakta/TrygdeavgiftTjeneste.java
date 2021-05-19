package no.nav.melosys.tjenester.gui.avklartefakta;

import io.swagger.annotations.Api;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.AvgiftsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregningsresultatDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterAvgiftsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterBeregningsgrunnlagDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = {"trygdeavgift"})
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
    public ResponseEntity<AvgiftsgrunnlagDto> oppdaterAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                                      @RequestBody OppdaterAvgiftsgrunnlagDto oppdaterAvgiftsgrunnlagDto
    ) {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            AvgiftsgrunnlagDto.av(
                trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(
                    behandlingID, oppdaterAvgiftsgrunnlagDto.til()
                )
            )
        );
    }

    @GetMapping("/grunnlag")
    public ResponseEntity<AvgiftsgrunnlagDto> hentAvgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) {
        tilgangService.sjekkTilgang(behandlingID);
        return ResponseEntity.ok(
            AvgiftsgrunnlagDto.av(
                trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingID)
            )
        );
    }

    @PutMapping("/beregning")
    public ResponseEntity<BeregningsresultatDto> oppdaterBeregningsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                                            @RequestBody OppdaterBeregningsgrunnlagDto oppdaterBeregningsgrunnlagDto) {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        trygdeavgiftsberegningService.oppdaterBeregningsgrunnlag(behandlingID, oppdaterBeregningsgrunnlagDto.til());

        return ResponseEntity.ok(
            BeregningsresultatDto.av(
                trygdeavgiftsberegningService.hentBeregningsresultat(behandlingID)
            )
        );
    }

    @GetMapping("/beregning")
    public ResponseEntity<BeregningsresultatDto> hentBeregningsresultat(@PathVariable("behandlingID") long behandlingID) {
        return ResponseEntity.ok(
            BeregningsresultatDto.av(
                trygdeavgiftsberegningService.hentBeregningsresultat(behandlingID)
            )
        );
    }
}
