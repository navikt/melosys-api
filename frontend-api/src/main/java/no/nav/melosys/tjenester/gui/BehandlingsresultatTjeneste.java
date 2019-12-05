package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@Api(tags = { "behandlingsresultat" })
@RestController("/behandlinger")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsresultatTjeneste extends RestTjeneste {

    private final TilgangService tilgangService;
    private BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public BehandlingsresultatTjeneste(BehandlingsresultatService behandlingsresultatService, TilgangService tilgangService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}/resultat")
    @ApiOperation(value = "Hent behandlingsresultat knyttet til en behandling",
        response = BehandlingsresultatDto.class)
    public ResponseEntity hentBehandlingsresultat(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        return ResponseEntity.ok(BehandlingsresultatDto.av(resultat));
    }
}
