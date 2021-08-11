package no.nav.melosys.tjenester.gui.saksflyt;


import io.swagger.annotations.Api;
import no.nav.melosys.service.unntaksperiode.EndretUnntaksperiodeGodkjenning;
import no.nav.melosys.service.unntaksperiode.Unntaksperiode;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.IkkeGodkjennUnntaksperiodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/unntaksperioder")
@Api(tags = {"saksflyt", "unntaksperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UnntakTjeneste {

    private final UnntaksperiodeService unntaksperiodeService;

    @Autowired
    public UnntakTjeneste(UnntaksperiodeService unntaksperiodeService) {
        this.unntaksperiodeService = unntaksperiodeService;
    }

    @PostMapping("{behandlingID}/ikkegodkjenn")
    public ResponseEntity<Void> ikkeGodkjennUnntaksperiode(@PathVariable("behandlingID") Long behandlingId, @RequestBody IkkeGodkjennUnntaksperiodeDto ikkeGodkjennUnntaksperiodeDto) {
        unntaksperiodeService.ikkeGodkjennPeriode(behandlingId, ikkeGodkjennUnntaksperiodeDto.ikkeGodkjentBegrunnelseKoder(), ikkeGodkjennUnntaksperiodeDto.begrunnelseFritekst());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{behandlingID}/godkjenn")
    public ResponseEntity<Void> godkjennUnntaksperiode(@PathVariable("behandlingID") Long behandlingId, @RequestBody GodkjennUnntaksperiodeDto godkjennUnntaksperiodeDto) {
        if (godkjennUnntaksperiodeDto.endretPeriode().erTom()) {
            unntaksperiodeService.godkjennPeriode(behandlingId, godkjennUnntaksperiodeDto.varsleUtland(), godkjennUnntaksperiodeDto.fritekst());
        } else {
            unntaksperiodeService.godkjennOgEndrePeriode(behandlingId, new EndretUnntaksperiodeGodkjenning(
                godkjennUnntaksperiodeDto.varsleUtland(),
                godkjennUnntaksperiodeDto.fritekst(),
                new Unntaksperiode(godkjennUnntaksperiodeDto.endretPeriode().getFom(), godkjennUnntaksperiodeDto.endretPeriode().getTom())
            ));
        }

        return ResponseEntity.noContent().build();
    }
}
