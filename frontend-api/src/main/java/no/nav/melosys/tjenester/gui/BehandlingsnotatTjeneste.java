package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsnotat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsnotatService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatGetDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BehandlingsnotatTjeneste.class);

    private final BehandlingsnotatService behandlingsnotatService;
    private final SaksbehandlerService saksbehandlerService;
    private final TilgangService tilgangService;

    public BehandlingsnotatTjeneste(BehandlingsnotatService behandlingsnotatService, SaksbehandlerService saksbehandlerService, TilgangService tilgangService) {
        this.behandlingsnotatService = behandlingsnotatService;
        this.saksbehandlerService = saksbehandlerService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("/{saksnummer}/notater")
    @ApiOperation(value = "Henter alle notater knyttet til behandlinger for fagsaken",
        response = BehandlingsnotatGetDto.class,
        responseContainer = "List")
    public ResponseEntity<Collection<BehandlingsnotatGetDto>> hentBehandlingsnotaterForFagsak(@PathVariable("saksnummer") String saksnummer)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkSak(saksnummer);

        Collection<BehandlingsnotatGetDto> notater = behandlingsnotatService.hentNotatForFagsak(saksnummer)
            .stream()
            .map(this::lagBehandlingsnotatGetDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(notater);
    }

    @PostMapping("/{saksnummer}/notater")
    @ApiOperation(value = "Oppretter et nytt notat på fagsaken sin aktive behandling",
        response = BehandlingsnotatGetDto.class)
    public ResponseEntity<BehandlingsnotatGetDto> opprettBehandlingsnotatForFagsak(@PathVariable("saksnummer") String saksnummer,
                                                                                   @RequestBody BehandlingsnotatPostDto behandlingsnotatPostDto)
        throws FunksjonellException, TekniskException {
        tilgangService.sjekkSak(saksnummer);
        Behandlingsnotat behandlingsnotat = behandlingsnotatService.opprettNotat(saksnummer, behandlingsnotatPostDto.getTekst());
        return ResponseEntity.ok(
            lagBehandlingsnotatGetDto(behandlingsnotat)
        );
    }

    @PutMapping("/{saksnummer}/notater/{notatID}")
    @ApiOperation(value = "Oppdaterer tekst på et notat",
        response = BehandlingsnotatGetDto.class)
    public ResponseEntity<BehandlingsnotatGetDto> oppdaterBehandlingsnotat(@PathVariable("saksnummer") String saksnummer,
                                                                           @PathVariable("notatID") Long notatID,
                                                                           @RequestBody BehandlingsnotatPostDto behandlingsnotatPostDto)
        throws FunksjonellException, TekniskException {
        tilgangService.sjekkSak(saksnummer);
        return ResponseEntity.ok(
            lagBehandlingsnotatGetDto(behandlingsnotatService.oppdaterNotat(notatID, behandlingsnotatPostDto.getTekst()))
        );
    }

    private BehandlingsnotatGetDto lagBehandlingsnotatGetDto(Behandlingsnotat behandlingsnotat) {
        return new BehandlingsnotatGetDto(
            behandlingsnotat,
            behandlingsnotatService.kanRedigereNotat(behandlingsnotat),
            navnEllerIdent(behandlingsnotat.getRegistrertAv())
        );
    }

    private String navnEllerIdent(String ident) {
        try {
            return saksbehandlerService.finnNavnForIdent(ident).orElse(ident);
        } catch (TekniskException e) {
            log.warn("Feil ved henting av navn for ident", e);
            return ident;
        }
    }
}
