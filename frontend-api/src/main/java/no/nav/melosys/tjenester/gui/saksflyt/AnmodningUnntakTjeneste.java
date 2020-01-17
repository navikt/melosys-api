package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.dto.AnmodningUnntakDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/anmodningsperioder")
@Api(tags = {"saksflyt", "anmodningsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningUnntakTjeneste {
    private final AnmodningUnntakService anmodningUnntakService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningUnntakTjeneste(AnmodningUnntakService anmodningUnntakService, TilgangService tilgangService) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/bestill")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public ResponseEntity anmodningOmUnntak(@PathVariable("behandlingID") long behandlingID, @RequestBody AnmodningUnntakDto anmodningUnntakDto) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID, anmodningUnntakDto.getMottakerinstitusjon());
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "{behandlingID}/svar", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Sender et svar på anmodning om unntak basert på AnmodningsperiodeSvar som er registrert på behandlingen")
    public ResponseEntity svar(@PathVariable("behandlingID") long behandlingID) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntakSvar(behandlingID);
        return ResponseEntity.ok().build();
    }
}
