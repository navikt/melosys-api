package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
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
@RequestMapping("/oppgaver")
@Api(tags = "oppgaver")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OppgaveTjeneste {
    private static final Logger log = LoggerFactory.getLogger(OppgaveTjeneste.class);

    private final Oppgaveplukker oppgaveplukker;
    private final OppgaveService oppgaveService;

    @Autowired
    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker, OppgaveService oppgaveService) {
        this.oppgaveplukker = oppgaveplukker;
        this.oppgaveService = oppgaveService;
    }

    @PostMapping("/plukk")
    @ApiOperation(value = "Plukker fra neste oppgave fra Oppgave som saksbehandler skal arbeide med.", response = PlukketOppgaveDto.class)
    public ResponseEntity plukkOppgave(@RequestBody PlukkOppgaveInnDto plukkDto) throws FunksjonellException, TekniskException {
        String ident = SubjectHandler.getInstance().getUserID();

        Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto);

        if (plukket.isPresent()) {
            Oppgave oppgave = plukket.get();
            PlukketOppgaveDto dto = new PlukketOppgaveDto();

            dto.setOppgaveID(oppgave.getOppgaveId());
            if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling()) {
                dto.setSaksnummer(oppgave.getSaksnummer());
            }

            Behandling behandling = oppgaveService.hentSistAktiveBehandling(oppgave.getSaksnummer());
            dto.setBehandlingID(behandling.getId());
            dto.setBehandlingstype(behandling.getType().getKode());
            dto.setJournalpostID(oppgave.getJournalpostId());

            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/tilbakelegg")
    @ApiOperation(value = "Legger tilbake oppgave knyttet til gitt behandlingID i GSAK.")
    public ResponseEntity leggTilbakeOppgave(@RequestBody TilbakeleggingDto tilbakelegging) throws FunksjonellException, TekniskException {
        String ident = SubjectHandler.getInstance().getUserID();
        oppgaveplukker.leggTilbakeOppgave(ident, tilbakelegging);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/oversikt")
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt en gitt saksbehandler.",
        response = OppgaveOversiktDto.class)
    public ResponseEntity mineOppgaver() throws TekniskException, FunksjonellException {
        String ident = SubjectHandler.getInstance().getUserID();
        List<OppgaveDto> oppgaveDtoListe;
        oppgaveDtoListe = oppgaveService.hentOppgaverMedAnsvarlig(ident);
        OppgaveOversiktDto oversiktDto = new OppgaveOversiktDto();
        List<JournalfoeringsoppgaveDto> journalføring = new ArrayList<>();
        List<BehandlingsoppgaveDto> saksbehandling = new ArrayList<>();

        for (OppgaveDto oppgaveDto : oppgaveDtoListe) {
            if (oppgaveDto instanceof JournalfoeringsoppgaveDto) {
                journalføring.add((JournalfoeringsoppgaveDto) oppgaveDto);
            } else if (oppgaveDto instanceof BehandlingsoppgaveDto) {
                saksbehandling.add((BehandlingsoppgaveDto) oppgaveDto);
            } else {
                log.warn("Ukjent oppgavetype {}: ", oppgaveDto.getClass().getSimpleName());
            }
        }
        oversiktDto.setJournalforing(journalføring);
        oversiktDto.setSaksbehandling(saksbehandling);
        return ResponseEntity.ok(oversiktDto);
    }

    @GetMapping("/sok")
    @ApiOperation(
        value = "Søk etter oppgaver knyttet til et fødselsnummer eller d-nummer",
        response = no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto.class,
        responseContainer = "List")
    public ResponseEntity søkOppgaverMedBrukerID(@RequestParam("fnr") String fnr)
        throws FunksjonellException, TekniskException {
        if (fnr == null) {
            throw new FunksjonellException("Fødselsnummer eller D-nummer mangler.");
        }
        try {
            return ResponseEntity.ok(oppgaveService.finnOppgaverMedBrukerID(fnr).stream()
                .map(no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto::av).collect(Collectors.toList()));
        } catch (IkkeFunnetException e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
