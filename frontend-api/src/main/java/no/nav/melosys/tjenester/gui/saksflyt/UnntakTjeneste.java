package no.nav.melosys.tjenester.gui.saksflyt;


import io.swagger.annotations.Api;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
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
    public ResponseEntity<Void> ikkeGodkjennUnntaksperiode(@PathVariable("behandlingID") Long behandlingId, @RequestBody VurderUnntaksperiodeDto vurderUnntaksperiodeDto) {
        unntaksperiodeService.ikkeGodkjennPeriode(behandlingId, vurderUnntaksperiodeDto.ikkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.begrunnelseFritekst());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{behandlingID}/godkjenn")
    public ResponseEntity<Void> godkjennUnntaksperiode(
        @PathVariable("behandlingID") Long behandlingId,
        @RequestBody GodkjennUnntaksperiodeDto godkjennUnntaksperiodeDto
    ) {
        unntaksperiodeService.godkjennPeriode(behandlingId, godkjennUnntaksperiodeDto.varsleUtland(), godkjennUnntaksperiodeDto.fritekst());
        return ResponseEntity.noContent().build();
    }
}
