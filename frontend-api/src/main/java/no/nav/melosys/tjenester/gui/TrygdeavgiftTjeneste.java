package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregningsresultatDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterBeregningsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto;
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
    private final Aksesskontroll aksesskontroll;

    public TrygdeavgiftTjeneste(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                TrygdeavgiftsberegningService trygdeavgiftsberegningService,
                                Aksesskontroll aksesskontroll) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.trygdeavgiftsberegningService = trygdeavgiftsberegningService;
        this.aksesskontroll = aksesskontroll;
    }

    @PutMapping("/grunnlag")
    public ResponseEntity<TrygdeavgiftsgrunnlagDto> oppdaterTrygdeavgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                                                  @RequestBody TrygdeavgiftsgrunnlagDto trygdeavgiftsgrunnlagDto
    ) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        return ResponseEntity.ok(
            TrygdeavgiftsgrunnlagDto.av(
                trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(behandlingID, trygdeavgiftsgrunnlagDto.tilRequest())
            )
        );
    }

    @GetMapping("/grunnlag")
    public ResponseEntity<TrygdeavgiftsgrunnlagDto> hentTrygdeavgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        var grunnlag = trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(behandlingID);
        return grunnlag == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(TrygdeavgiftsgrunnlagDto.av(grunnlag));
    }

    @PutMapping("/beregning")
    public ResponseEntity<BeregningsresultatDto> oppdaterBeregningsgrunnlag(@PathVariable("behandlingID") long behandlingID,
                                                                            @RequestBody OppdaterBeregningsgrunnlagDto oppdaterBeregningsgrunnlagDto) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);
        trygdeavgiftsberegningService.oppdaterBeregningsgrunnlag(behandlingID, oppdaterBeregningsgrunnlagDto.til());

        return ResponseEntity.ok(
            BeregningsresultatDto.av(
                trygdeavgiftsberegningService.hentBeregningsresultat(behandlingID)
            )
        );
    }

    @GetMapping("/beregning")
    public ResponseEntity<BeregningsresultatDto> hentBeregningsresultat(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return ResponseEntity.ok(
            BeregningsresultatDto.av(
                trygdeavgiftsberegningService.hentBeregningsresultat(behandlingID)
            )
        );
    }
}
