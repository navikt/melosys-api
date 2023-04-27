package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto;
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
            new TrygdeavgiftsgrunnlagDto(
                trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(behandlingID, trygdeavgiftsgrunnlagDto.tilRequest())
            )
        );
    }

    @GetMapping("/grunnlag")
    public ResponseEntity<TrygdeavgiftsgrunnlagDto> hentTrygdeavgiftsgrunnlag(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        var grunnlag = trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(behandlingID);
        return grunnlag == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(new TrygdeavgiftsgrunnlagDto(grunnlag));
    }

    @PutMapping("/beregning")
    public ResponseEntity<BeregnetTrygdeavgiftDto> beregnTrygdeavgift(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(
                trygdeavgiftsberegningService.beregnTrygdeavgift(behandlingID)
            )
        );
    }

    @GetMapping("/beregning")
    public ResponseEntity<BeregnetTrygdeavgiftDto> hentTrygdeavgift(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(
                trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(behandlingID)
            )
        );
    }
}
