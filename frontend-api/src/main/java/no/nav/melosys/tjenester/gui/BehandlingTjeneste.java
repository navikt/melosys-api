package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;

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
        String saksbehandlerID = SubjectHandler.getInstance().getUserID();
        log.debug("Saksbehandler {} ber om å hente behandling {}.", saksbehandlerID, behandlingID);
        aksesskontroll.autoriser(behandlingID);

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        behandlingService.oppdaterBehandlingsstatusHvisTilhørendeSaksbehandler(behandling, saksbehandlerID);
        BehandlingDto behandlingDto = tilBehandlingDto(behandling, saksbehandlerID);
        return ResponseEntity.ok(behandlingDto);
    }

    @GetMapping("{behandlingID}/mulige-statuser")
    @ApiOperation("Hent mulige nye behandlingsstatuser for en behandling")
    public ResponseEntity<Collection<Behandlingsstatus>> hentMuligeStatuser(@PathVariable("behandlingID") long behandlingID) {
        log.debug("Saksbehandler {} ber om å hente mulige nye behandlingsstatuser for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriser(behandlingID);

        return ResponseEntity.ok(behandlingService.hentMuligeStatuser(behandlingID));
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
        behandlingOppsummeringDto.setBehandlingsresultattype(behandlingsresultat.getType());
        return behandlingOppsummeringDto;
    }
}
