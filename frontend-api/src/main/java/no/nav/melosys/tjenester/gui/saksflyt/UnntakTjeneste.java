package no.nav.melosys.tjenester.gui.saksflyt;


import io.swagger.annotations.Api;
import no.nav.melosys.service.tilgang.Aksesskontroll;
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
    private final Aksesskontroll aksesskontroll;

    @Autowired
    public UnntakTjeneste(UnntaksperiodeService unntaksperiodeService, Aksesskontroll aksesskontroll) {
        this.unntaksperiodeService = unntaksperiodeService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping("{behandlingID}/ikkegodkjenn")
    public ResponseEntity<Void> ikkeGodkjennUnntaksperiode(
        @PathVariable("behandlingID") Long behandlingId,
        @RequestBody IkkeGodkjennUnntaksperiodeDto ikkeGodkjennUnntaksperiodeDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        unntaksperiodeService.ikkeGodkjennPeriode(behandlingId, ikkeGodkjennUnntaksperiodeDto.ikkeGodkjentBegrunnelseKoder(), ikkeGodkjennUnntaksperiodeDto.begrunnelseFritekst());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{behandlingID}/godkjenn")
    public ResponseEntity<Void> godkjennUnntaksperiode(
        @PathVariable("behandlingID") Long behandlingId,
        @RequestBody GodkjennUnntaksperiodeDto godkjennUnntaksperiodeDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        unntaksperiodeService.godkjennPeriode(behandlingId, godkjennUnntaksperiodeDto.til());
        return ResponseEntity.noContent().build();
    }
}
