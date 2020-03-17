package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsnotatService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.BehandlingnotatGetDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsnotatTjeneste {

    private final BehandlingsnotatService behandlingsnotatService;
    private final TilgangService tilgangService;

    public BehandlingsnotatTjeneste(BehandlingsnotatService behandlingsnotatService, TilgangService tilgangService) {
        this.behandlingsnotatService = behandlingsnotatService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("/{saksnummer}/notat")
    @ApiOperation(value = "Henter alle notater knyttet til behandlinger i for fagsaken",
        response = BehandlingnotatGetDto.class,
        responseContainer = "List")
    public ResponseEntity hentBehandlingsnotaterForFagsak(@PathVariable("saksnummer") String saksnummer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        tilgangService.sjekkSak(saksnummer);

        Collection<BehandlingnotatGetDto> notater = behandlingsnotatService.hentNotatForFagsak(saksnummer)
            .stream()
            .map(BehandlingnotatGetDto::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(notater);
    }

    @PostMapping("/{saksnummer}/notat")
    @ApiOperation(value = "Oppretter et nytt notat på fagsaken sin aktive behandling",
        response = BehandlingnotatGetDto.class)
    public ResponseEntity opprettBehandlingsnotatForFagsak(@PathVariable("saksnummer") String saksnummer,
                                                @RequestBody BehandlingsnotatPostDto behandlingsnotatPostDto) throws FunksjonellException, TekniskException {
        tilgangService.sjekkSak(saksnummer);
        return ResponseEntity.ok(
            new BehandlingnotatGetDto(behandlingsnotatService.opprettNotat(saksnummer, behandlingsnotatPostDto.getTekst()))
        );
    }

    @PutMapping("/{saksnummer}/notat/{notatID}")
    @ApiOperation(value = "Oppdaterer tekst på et notat",
        response = BehandlingnotatGetDto.class)
    public ResponseEntity oppdaterBehandlingsnotat(@PathVariable("saksnummer") String saksnummer,
                                        @PathVariable("notatID") Long notatID,
                                        @RequestBody BehandlingsnotatPostDto behandlingsnotatPostDto) throws FunksjonellException, TekniskException {
        tilgangService.sjekkSak(saksnummer);
        return ResponseEntity.ok(
            new BehandlingnotatGetDto(behandlingsnotatService.oppdaterNotat(notatID, behandlingsnotatPostDto.getTekst()))
        );
    }
}
