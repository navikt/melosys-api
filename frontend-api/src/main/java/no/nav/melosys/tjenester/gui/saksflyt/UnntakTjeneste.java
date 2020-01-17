package no.nav.melosys.tjenester.gui.saksflyt;


import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
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
    public ResponseEntity ikkeGodkjennUnntaksperiode(@PathVariable("behandlingID") Long behandlingId, @RequestBody VurderUnntaksperiodeDto vurderUnntaksperiodeDto) throws FunksjonellException, TekniskException {
        unntaksperiodeService.ikkeGodkjennPeriode(behandlingId, vurderUnntaksperiodeDto.getIkkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.getBegrunnelseFritekst());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{behandlingID}/godkjenn", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity godkjennUnntaksperiode(@PathVariable("behandlingID") Long behandlingId) throws FunksjonellException, TekniskException {
        unntaksperiodeService.godkjennPeriode(behandlingId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{behandlingID}/innhentinfo", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity innhentInformasjonUnntaksperiode(@PathVariable("behandlingID") Long behandlingId) throws FunksjonellException, TekniskException {
        unntaksperiodeService.behandlingUnderAvklaring(behandlingId);
        return ResponseEntity.noContent().build();
    }
}