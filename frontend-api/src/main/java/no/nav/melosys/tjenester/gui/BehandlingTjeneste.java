package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.behandling.EndreBehandlingstemaService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.tildto.SaksopplysningerTilDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.stream.Collectors;

@Protected
@RestController
@RequestMapping("/behandlinger")
@Api(tags = { "behandlinger" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BehandlingTjeneste.class);

    private final BehandlingService behandlingService;
    private final SaksopplysningerTilDto saksopplysningerTilDto;
    private final TilgangService tilgangService;
    private final SaksbehandlerService saksbehandlerService;
    private final EndreBehandlingstemaService endreBehandlingstemaService;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingService,
                              SaksopplysningerTilDto saksopplysningerTilDto,
                              TilgangService tilgangService,
                              SaksbehandlerService saksbehandlerService,
                              EndreBehandlingstemaService endreBehandlingstemaService) {
        this.behandlingService = behandlingService;
        this.saksopplysningerTilDto = saksopplysningerTilDto;
        this.tilgangService = tilgangService;
        this.saksbehandlerService = saksbehandlerService;
        this.endreBehandlingstemaService = endreBehandlingstemaService;
    }

    @PostMapping("{behandlingID}/status")
    @ApiOperation("Oppdaterer status for en behandling. " +
        "Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.")
    public ResponseEntity<Void> oppdaterStatus(@PathVariable("behandlingID") long behandlingID,
                                               @RequestBody BehandlingsstatusDto status)
        throws MelosysException {
        log.info("Saksbehandler {} ber om å endre status for behandling {} til {}.", SubjectHandler.getInstance().getUserID(),
            behandlingID, status.getBehandlingsstatus().getKode());
        tilgangService.sjekkTilgang(behandlingID);
        behandlingService.brukerOppdaterStatus(behandlingID, status.getBehandlingsstatus());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Knytt medlemsperioder fra MEDL til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity<TidligereMedlemsperioderDto> knyttMedlemsperioder(@PathVariable("behandlingID") long behandlingID,
                                                                            @RequestBody TidligereMedlemsperioderDto tidligereMedlemsperioder)
        throws MelosysException {
        log.info("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder);
        return ResponseEntity.ok(tidligereMedlemsperioder);
    }

    @GetMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Hent medlemsperioder knyttet til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity<TidligereMedlemsperioderDto> hentMedlemsperioder(@PathVariable("behandlingID") long behandlingID)
        throws MelosysException {
        log.debug("Saksbehandler {} ber om å hente medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        return ResponseEntity.ok(tidligereMedlemsperioderDto);
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Hent en spesifikk behandling", response = BehandlingDto.class)
    public ResponseEntity<BehandlingDto> hentBehandling(@PathVariable("behandlingID") long behandlingID)
        throws MelosysException {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        log.debug("Saksbehandler {} ber om å hente behandling {}.", saksbehandler, behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);
        BehandlingDto behandlingDto = tilBehandlingDto(behandling, saksbehandler);
        return ResponseEntity.ok(behandlingDto);
    }

    @GetMapping("{behandlingID}/muligeBehandlingstema")
    @ApiOperation(value = "Hent mulige nye behandlingstema for en behandling")
    public ResponseEntity<List<String>> hentEndreBehandlingstema(@PathVariable("behandlingID") long behandlingsID)
        throws MelosysException{
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingstema for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingsID);
        tilgangService.sjekkTilgang(behandlingsID);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(behandlingsID);
        return ResponseEntity.ok(muligeBehandlingstema.stream().map(Behandlingstema::getKode).collect(Collectors.toList()));
    }

    @PostMapping("{behandlingID}/endreBehandlingstema")
    @ApiOperation(value = "Endre behandlingstema for en gitt behandling")
    public ResponseEntity<Void> endreBehandlingstema(@PathVariable("behandlingID") long behandlingsID, @RequestBody EndreBehandlingstemaDto nyttTema)
        throws MelosysException{
        log.debug("Saksbehandler {} ber om å sette behandlingstema for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingsID, nyttTema);
        tilgangService.sjekkTilgang(behandlingsID);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(behandlingsID, Behandlingstema.valueOf(nyttTema.getBehandlingstema()));
        return ResponseEntity.noContent().build();
    }


    private BehandlingDto tilBehandlingDto(Behandling behandling, String saksbehandler) throws MelosysException {
        BehandlingDto behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setRedigerbart(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler));
        behandlingDto.setOppsummering(tilOppsummeringDto(behandling));
        SaksopplysningerDto saksopplysningerDto = saksopplysningerTilDto.getSaksopplysningerDto(behandling.getSaksopplysninger(), behandling);
        behandlingDto.setSaksopplysninger(saksopplysningerDto);
        return behandlingDto;
    }

    private BehandlingOppsummeringDto tilOppsummeringDto(Behandling behandling) throws TekniskException, IkkeFunnetException {
        BehandlingOppsummeringDto behandlingOppsummeringDto = new BehandlingOppsummeringDto();
        behandlingOppsummeringDto.setBehandlingsstatus(behandling.getStatus());
        behandlingOppsummeringDto.setBehandlingstype(behandling.getType());
        behandlingOppsummeringDto.setBehandlingstema(behandling.getTema());
        behandlingOppsummeringDto.setEndretDato(behandling.getEndretDato());
        behandlingOppsummeringDto.setEndretAvNavn(saksbehandlerService.hentNavnForIdent(behandling.getEndretAv()));
        behandlingOppsummeringDto.setRegistrertDato(behandling.getRegistrertDato());
        behandlingOppsummeringDto.setSisteOpplysningerHentetDato(behandling.getSistOpplysningerHentetDato());
        return behandlingOppsummeringDto;
    }
}
