package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.OppgaveFristFerdigstillelseEndretEvent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.EndreBehandlingstemaService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.tildto.SaksopplysningerTilDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/behandlinger")
@Api(tags = {"behandlinger"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BehandlingTjeneste.class);

    private final BehandlingService behandlingService;
    private final SaksopplysningerTilDto saksopplysningerTilDto;
    private final TilgangService tilgangService;
    private final SaksbehandlerService saksbehandlerService;
    private final EndreBehandlingstemaService endreBehandlingstemaService;
    private final OppgaveService oppgaveService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingService,
                              SaksopplysningerTilDto saksopplysningerTilDto,
                              TilgangService tilgangService,
                              SaksbehandlerService saksbehandlerService,
                              EndreBehandlingstemaService endreBehandlingstemaService,
                              OppgaveService oppgaveService,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.behandlingService = behandlingService;
        this.saksopplysningerTilDto = saksopplysningerTilDto;
        this.tilgangService = tilgangService;
        this.saksbehandlerService = saksbehandlerService;
        this.endreBehandlingstemaService = endreBehandlingstemaService;
        this.oppgaveService = oppgaveService;
        this.applicationEventPublisher = applicationEventPublisher;
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

    @GetMapping("{behandlingID}/muligeStatuser")
    @ApiOperation("Hent mulige nye behandlingsstatuser for en behandling")
    public ResponseEntity<Collection<Behandlingsstatus>> hentMuligeStatuser(@PathVariable("behandlingID") long behandlingID)
        throws MelosysException {
        log.info("Saksbehandler {} ber om å hente mulige nye behandlingsstatuser for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgangService.sjekkTilgang(behandlingID);

        return ResponseEntity.ok(behandlingService.hentMuligeStatuser(behandlingID));
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
    @JsonView(DokumentView.FrontendApi.class)
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
    public ResponseEntity<List<Behandlingstema>> hentEndreBehandlingstema(@PathVariable("behandlingID") long behandlingsID)
        throws MelosysException {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingstema for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingsID);
        try {
            tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(behandlingsID);
        } catch (FunksjonellException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(behandlingsID);
        return ResponseEntity.ok(muligeBehandlingstema);
    }

    @PostMapping("{behandlingID}/endreBehandlingstema")
    @ApiOperation(value = "Endre behandlingstema for en gitt behandling")
    public ResponseEntity<Void> endreBehandlingstema(@PathVariable("behandlingID") long behandlingsID, @RequestBody EndreBehandlingstemaDto endreBehandlingstemaDto)
        throws MelosysException {
        log.debug("Saksbehandler {} ber om å sette behandlingstema for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingsID, endreBehandlingstemaDto);
        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(behandlingsID);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(behandlingsID, Behandlingstema.valueOf(endreBehandlingstemaDto.getBehandlingstema()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/endreBehandlingsfrist")
    @ApiOperation("Endre behandlingsfristen for en gitt behandling samt tilhørende oppgave i Gosys")
    public ResponseEntity<Void> endreBehandlingsfrist(@PathVariable("behandlingID") long behandlingID, @RequestBody EndreBehandlingsfristDto endreBehandlingsfristDto) throws MelosysException {
        log.debug("Saksbehandler {} ber om å sette behandlingsfrist for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingID, endreBehandlingsfristDto.getBehandlingsfrist());
        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        behandlingService.endreBehandlingsfrist(behandling, endreBehandlingsfristDto.getBehandlingsfrist());

        String oppgaveId = oppgaveService.hentOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer()).getOppgaveId();
        applicationEventPublisher.publishEvent(new OppgaveFristFerdigstillelseEndretEvent(oppgaveId, endreBehandlingsfristDto.getBehandlingsfrist()));
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

    private BehandlingOppsummeringDto tilOppsummeringDto(Behandling behandling) throws TekniskException {
        BehandlingOppsummeringDto behandlingOppsummeringDto = new BehandlingOppsummeringDto();
        behandlingOppsummeringDto.setBehandlingsstatus(behandling.getStatus());
        behandlingOppsummeringDto.setBehandlingstype(behandling.getType());
        behandlingOppsummeringDto.setBehandlingstema(behandling.getTema());
        behandlingOppsummeringDto.setEndretDato(behandling.getEndretDato());
        behandlingOppsummeringDto.setEndretAvNavn(saksbehandlerService.finnNavnForIdent(behandling.getEndretAv()).orElse(behandling.getEndretAv()));
        behandlingOppsummeringDto.setRegistrertDato(behandling.getRegistrertDato());
        behandlingOppsummeringDto.setSisteOpplysningerHentetDato(behandling.getSistOpplysningerHentetDato());
        behandlingOppsummeringDto.setSvarFrist(behandling.getDokumentasjonSvarfristDato());
        behandlingOppsummeringDto.setBehandlingsfrist(behandling.getBehandlingsfrist());
        return behandlingOppsummeringDto;
    }
}
