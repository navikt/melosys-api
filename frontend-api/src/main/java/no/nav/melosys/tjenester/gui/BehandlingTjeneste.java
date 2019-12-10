package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.TilgangService;
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

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingService, SaksopplysningerTilDto saksopplysningerTilDto, TilgangService tilgangService) {
        this.behandlingService = behandlingService;
        this.saksopplysningerTilDto = saksopplysningerTilDto;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/status")
    @ApiOperation("Oppdaterer status for en behandling. " +
        "Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.")
    public ResponseEntity oppdaterStatus(@PathVariable("behandlingID") long behandlingID,
                               @RequestBody BehandlingsstatusDto status) throws FunksjonellException, TekniskException {
        log.info("Saksbehandler {} ber om å endre status for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingID, status.getBehandlingsstatus().getKode());
        tilgangService.sjekkTilgang(behandlingID);
        behandlingService.oppdaterStatus(behandlingID, status.getBehandlingsstatus());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Knytt medlemsperioder fra MEDL til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity knyttMedlemsperioder(@PathVariable("behandlingID") long behandlingID,
                                               @RequestBody TidligereMedlemsperioderDto tidligereMedlemsperioder) throws FunksjonellException, TekniskException {
        log.info("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder);
        return ResponseEntity.ok(tidligereMedlemsperioder);
    }

    @GetMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Hent medlemsperioder knyttet til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity hentMedlemsperioder(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        log.debug("Saksbehandler {} ber om å hente medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        return ResponseEntity.ok(tidligereMedlemsperioderDto);
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Hent en spesifikk behandling",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity hentBehandling(@PathVariable("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        log.debug("Saksbehandler {} ber om å hente behandling {}.", saksbehandler, behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);
        BehandlingDto behandlingDto = tilBehandlingDto(behandling, saksbehandler);
        return ResponseEntity.ok(behandlingDto);
    }

    private BehandlingDto tilBehandlingDto(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BehandlingDto behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setRedigerbart(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler));
        behandlingDto.setOppsummering(tilOppsummeringDto(behandling));
        SaksopplysningerDto saksopplysningerDto = saksopplysningerTilDto.getSaksopplysningerDto(behandling.getSaksopplysninger(), behandling);
        behandlingDto.setSaksopplysninger(saksopplysningerDto);
        return behandlingDto;
    }

    private BehandlingOppsummeringDto tilOppsummeringDto(Behandling behandling) {
        BehandlingOppsummeringDto behandlingOppsummeringDto = new BehandlingOppsummeringDto();
        behandlingOppsummeringDto.setBehandlingsstatus(behandling.getStatus());
        behandlingOppsummeringDto.setBehandlingstype(behandling.getType());
        behandlingOppsummeringDto.setEndretDato(behandling.getEndretDato());
        behandlingOppsummeringDto.setRegistrertDato(behandling.getRegistrertDato());
        behandlingOppsummeringDto.setSisteOpplysningerHentetDato(behandling.getSistOpplysningerHentetDato());
        return behandlingOppsummeringDto;
    }
}
