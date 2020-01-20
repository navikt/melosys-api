package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"utpekingsperioder"})
@RequestMapping("/utpekingsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtpekingsperiodeTjeneste {

    private final TilgangService tilgangService;

    @Autowired
    public UtpekingsperiodeTjeneste(TilgangService tilgangService) {
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter utpekingsperioder for en gitt behandling", response = UtpekingsperioderDto.class)
    public UtpekingsperioderDto hentAnmodningsperioder(@PathVariable("behandlingID") long behandlingID)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        tilgangService.sjekkTilgang(behandlingID);
        return null;
    }

    @PostMapping("{behandlingID}")
    @ApiOperation("Lagrer utpekingssperioder for en gitt behandling.")
    public UtpekingsperioderDto lagreAnmodningsperioder(@PathVariable("behandlingID") long behandlingID,
                                                           @RequestBody UtpekingsperioderDto utpekingsperioderDto)
        throws TekniskException, FunksjonellException {

        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        return null;
    }
}
