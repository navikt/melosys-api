package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.EndreBehandlingService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto;
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
@Api(tags = {"behandlinger"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BehandlingTjeneste.class);

    private final BehandlingService behandlingService;
    private final SaksopplysningerTilDto saksopplysningerTilDto;
    private final SaksbehandlerService saksbehandlerService;
    private final EndreBehandlingService endreBehandlingService;
    private final Aksesskontroll aksesskontroll;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingService,
                              SaksopplysningerTilDto saksopplysningerTilDto,
                              SaksbehandlerService saksbehandlerService,
                              EndreBehandlingService endreBehandlingService,
                              Aksesskontroll aksesskontroll) {
        this.behandlingService = behandlingService;
        this.saksopplysningerTilDto = saksopplysningerTilDto;
        this.saksbehandlerService = saksbehandlerService;
        this.endreBehandlingService = endreBehandlingService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation("Endre behandling")
    public ResponseEntity<Void> endreBehandling(@PathVariable long behandlingID,
                                                @RequestBody EndreBehandlingDto endreBehandling) {
        log.info("Saksbehandler {} ber om å endre behandling {} med {}", SubjectHandler.getInstance().getUserID(), endreBehandling);
        aksesskontroll.autoriser(behandlingID);

        var sakstype = endreBehandling.sakstype() == null ? null : Sakstyper.valueOf(endreBehandling.sakstype());
        var behandlingstype = endreBehandling.behandlingstype() == null ? null : Behandlingstyper.valueOf(endreBehandling.behandlingstype());
        var behandlingstema = endreBehandling.behandlingstema() == null ? null : Behandlingstema.valueOf(endreBehandling.behandlingstema());
        var behandlingsstatus = endreBehandling.behandlingsstatus() == null ? null : Behandlingsstatus.valueOf(endreBehandling.behandlingsstatus());

        endreBehandlingService.endreBehandling(behandlingID, sakstype, behandlingstype, behandlingstema, behandlingsstatus, endreBehandling.behandlingsfrist());
        return ResponseEntity.noContent().build();
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @PostMapping("{behandlingID}/status")
    @ApiOperation("Endre status for en gitt behandling.")
    public ResponseEntity<Void> endreStatus(@PathVariable("behandlingID") long behandlingID,
                                            @RequestBody EndreBehandlingsstatusDto status) {
        log.info("Saksbehandler {} ber om å endre status for behandling {} til {}.", SubjectHandler.getInstance().getUserID(),
            behandlingID, status.behandlingsstatus());
        aksesskontroll.autoriserSkriv(behandlingID);
        endreBehandlingService.endreStatus(behandlingID, Behandlingsstatus.valueOf(status.behandlingsstatus()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Knytt medlemsperioder fra MEDL til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity<TidligereMedlemsperioderDto> knyttMedlemsperioder(@PathVariable("behandlingID") long behandlingID,
                                                                            @RequestBody TidligereMedlemsperioderDto tidligereMedlemsperioder) {
        log.info("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriserSkriv(behandlingID);

        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder);
        return ResponseEntity.ok(tidligereMedlemsperioder);
    }

    @GetMapping("{behandlingID}/tidligeremedlemsperioder")
    @ApiOperation(value = "Hent medlemsperioder knyttet til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity<TidligereMedlemsperioderDto> hentMedlemsperioder(@PathVariable("behandlingID") long behandlingID) {
        log.debug("Saksbehandler {} ber om å hente medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        return ResponseEntity.ok(tidligereMedlemsperioderDto);
    }

    @GetMapping("{behandlingID}")
    @JsonView(DokumentView.FrontendApi.class)
    @ApiOperation(value = "Hent en spesifikk behandling", response = BehandlingDto.class)
    public ResponseEntity<BehandlingDto> hentBehandling(@PathVariable("behandlingID") long behandlingID) {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        log.debug("Saksbehandler {} ber om å hente behandling {}.", saksbehandler, behandlingID);
        aksesskontroll.autoriser(behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);
        BehandlingDto behandlingDto = tilBehandlingDto(behandling, saksbehandler);
        return ResponseEntity.ok(behandlingDto);
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @PostMapping("{behandlingID}/endreBehandlingstema")
    @ApiOperation(value = "Endre behandlingstema for en gitt behandling")
    public ResponseEntity<Void> endreBehandlingstema(@PathVariable("behandlingID") long behandlingsID, @RequestBody EndreBehandlingstemaDto endreBehandlingstemaDto) {
        log.debug("Saksbehandler {} ber om å sette behandlingstema for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingsID, endreBehandlingstemaDto);
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingsID);

        endreBehandlingService.endreBehandlingstemaTilBehandling(behandlingsID, Behandlingstema.valueOf(endreBehandlingstemaDto.behandlingstema()));
        return ResponseEntity.noContent().build();
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @PostMapping("{behandlingID}/behandlingsfrist")
    @ApiOperation("Endre behandlingsfristen for en gitt behandling samt tilhørende oppgave i Gosys")
    public ResponseEntity<Void> endreBehandlingsfrist(@PathVariable("behandlingID") long behandlingID, @RequestBody EndreBehandlingsfristDto endreBehandlingsfristDto) {
        log.debug("Saksbehandler {} ber om å sette behandlingsfrist for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingID, endreBehandlingsfristDto.behandlingsfrist());
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID);

        behandlingService.endreBehandlingsfrist(behandlingID, endreBehandlingsfristDto.behandlingsfrist());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{behandlingID}/mulige-statuser")
    @ApiOperation("Hent mulige nye behandlingsstatuser for en behandling")
    public ResponseEntity<Collection<Behandlingsstatus>> hentMuligeStatuser(@PathVariable("behandlingID") long behandlingID) {
        log.info("Saksbehandler {} ber om å hente mulige nye behandlingsstatuser for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(endreBehandlingService.hentMuligeStatuser(behandlingID));
    }

    @GetMapping("{behandlingID}/mulige-behandlingstema")
    @ApiOperation(value = "Hent mulige nye behandlingstema for en behandling")
    public ResponseEntity<List<Behandlingstema>> hentMuligeBehandlingstema(@PathVariable("behandlingID") long behandlingsID) {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingstema for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingsID);
        try {
            aksesskontroll.autoriser(behandlingsID);
        } catch (FunksjonellException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(behandlingsID);
        return ResponseEntity.ok(muligeBehandlingstema);
    }

    @GetMapping("{behandlingID}/mulige-typer")
    @ApiOperation("Hent mulige nye behandlingstyper for en behandling")
    public ResponseEntity<Collection<Behandlingstyper>> hentMuligeTyper(@PathVariable("behandlingID") long behandlingID) {
        log.info("Saksbehandler {} ber om å hente mulige nye behandlingstyper for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(endreBehandlingService.hentMuligeTyper(behandlingID));
    }


    private BehandlingDto tilBehandlingDto(Behandling behandling, String saksbehandler) {
        var behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setRedigerbart(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler));
        behandlingDto.setOppsummering(tilOppsummeringDto(behandling));
        var saksopplysningerDto = saksopplysningerTilDto.getSaksopplysningerDto(behandling.getSaksopplysninger(), behandling);
        behandlingDto.setSaksopplysninger(saksopplysningerDto);
        return behandlingDto;
    }

    private BehandlingOppsummeringDto tilOppsummeringDto(Behandling behandling) {
        var behandlingOppsummeringDto = new BehandlingOppsummeringDto();
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
