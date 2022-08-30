package no.nav.melosys.tjenester.gui;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Aksesskontroll aksesskontroll;
    private final BehandlingsresultatService behandlingsresultatService;

    public BehandlingTjeneste(BehandlingService behandlingService,
                              SaksopplysningerTilDto saksopplysningerTilDto,
                              SaksbehandlerService saksbehandlerService,
                              Aksesskontroll aksesskontroll,
                              BehandlingsresultatService behandlingsresultatService) {
        this.behandlingService = behandlingService;
        this.saksopplysningerTilDto = saksopplysningerTilDto;
        this.saksbehandlerService = saksbehandlerService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation("Endre behandling")
    public ResponseEntity<Void> endreBehandling(@PathVariable long behandlingID,
                                                @RequestBody EndreBehandlingDto endreBehandling) {
        log.debug("Saksbehandler {} ber om å endre behandling {} med {}", SubjectHandler.getInstance().getUserID(), behandlingID, endreBehandling);
        aksesskontroll.autoriser(behandlingID);

        behandlingService.endreBehandling(behandlingID, endreBehandling.sakstype(), endreBehandling.behandlingstype(),
            endreBehandling.behandlingstema(), endreBehandling.behandlingsstatus(), endreBehandling.behandlingsfrist());
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
        behandlingService.endreStatus(behandlingID, Behandlingsstatus.valueOf(status.behandlingsstatus()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/tidligere-medlemsperioder")
    @ApiOperation(value = "Knytt medlemsperioder fra MEDL til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public ResponseEntity<TidligereMedlemsperioderDto> knyttMedlemsperioder(@PathVariable("behandlingID") long behandlingID,
                                                                            @RequestBody TidligereMedlemsperioderDto tidligereMedlemsperioder) {
        log.debug("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriserSkriv(behandlingID);

        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder);
        return ResponseEntity.ok(tidligereMedlemsperioder);
    }

    @GetMapping("{behandlingID}/tidligere-medlemsperioder")
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

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
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

        behandlingService.endreBehandlingstemaTilBehandling(behandlingsID, Behandlingstema.valueOf(endreBehandlingstemaDto.behandlingstema()));
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

    @PutMapping("{behandlingID}/sett-til-ferdigbehandlet")
    @ApiOperation("Avslutt en gitt behandling med Ferdigbehandlet som resultat uten endring av saksstatus")
    public ResponseEntity<Void> avsluttNyVurderingMedFerdigbehandlet(@PathVariable("behandlingID") long behandlingID) {
        log.debug("Saksbehandler {} ber om å avslutte behandling {} uten endring av saksstatus", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID);

        behandlingService.settNyVurderingTilFerdigbehandlet(behandlingID);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("{behandlingID}/mulige-statuser")
    @ApiOperation("Hent mulige nye behandlingsstatuser for en behandling")
    public ResponseEntity<Collection<Behandlingsstatus>> hentMuligeStatuser(@PathVariable("behandlingID") long behandlingID) {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingsstatuser for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(behandlingService.hentMuligeStatuser(behandlingID));
    }

    @GetMapping("{behandlingID}/mulige-behandlingstema")
    @ApiOperation(value = "Hent mulige nye behandlingstema for en behandling")
    public ResponseEntity<Collection<Behandlingstema>> hentMuligeBehandlingstema(@PathVariable("behandlingID") long behandlingsID) {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingstema for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingsID);
        aksesskontroll.autoriser(behandlingsID);

        return ResponseEntity.ok(behandlingService.hentMuligeBehandlingstema(behandlingsID));
    }

    @GetMapping("{behandlingID}/mulige-typer")
    @ApiOperation("Hent mulige nye behandlingstyper for en behandling")
    public ResponseEntity<Collection<Behandlingstyper>> hentMuligeTyper(@PathVariable("behandlingID") long behandlingID) {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingstyper for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(behandlingService.hentMuligeTyper(behandlingID));
    }


    private BehandlingDto tilBehandlingDto(Behandling behandling, String saksbehandler) {
        var behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setRedigerbart(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler));
        behandlingDto.setOppsummering(tilOppsummeringDto(behandling));
        var saksopplysningerDto = saksopplysningerTilDto.getSaksopplysningerDto(behandling.getSaksopplysninger());
        behandlingDto.setSaksopplysninger(saksopplysningerDto);
        return behandlingDto;
    }

    private BehandlingOppsummeringDto tilOppsummeringDto(Behandling behandling) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

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
        behandlingOppsummeringDto.setBehandlingsresultattype(behandlingsresultat.getType());
        return behandlingOppsummeringDto;
    }
}
