package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.OppgaveSoek;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final OppgaveSoek oppgaveSoek;

    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker, OppgaveService oppgaveService, OppgaveSoek oppgaveSoek) {
        this.oppgaveplukker = oppgaveplukker;
        this.oppgaveService = oppgaveService;
        this.oppgaveSoek = oppgaveSoek;
    }

    @PostMapping("/plukk")
    @ApiOperation(value = "Plukker neste oppgave fra Oppgave som saksbehandler skal arbeide med.", response = PlukketOppgaveDto.class)
    public ResponseEntity<PlukketOppgaveDto> plukkOppgave(@RequestBody PlukkOppgaveInnDto plukkDto) {
        String ident = SubjectHandler.getInstance().getUserID();

        Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto);

        if (plukket.isPresent()) {
            Oppgave oppgave = plukket.get();
            PlukketOppgaveDto dto = new PlukketOppgaveDto();

            dto.setOppgaveID(oppgave.getOppgaveId());
            if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling() || oppgave.erVurderHenvendelse()) {
                dto.setSaksnummer(oppgave.getSaksnummer());
            }

            Behandling behandling = oppgaveService.hentSistAktiveBehandling(oppgave.getSaksnummer());
            dto.setBehandlingID(behandling.getId());
            dto.setBehandlingstype(behandling.getType().getKode());
            dto.setBehandlingstema(behandling.getTema().getKode());
            dto.setJournalpostID(oppgave.getJournalpostId());

            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping("/tilbakelegg")
    @ApiOperation(value = "Legger tilbake oppgave knyttet til gitt behandlingID i GSAK.")
    public ResponseEntity<Void> leggTilbakeOppgave(@RequestBody TilbakeleggingDto tilbakelegging) {
        String ident = SubjectHandler.getInstance().getUserID();
        oppgaveplukker.leggTilbakeOppgave(ident, tilbakelegging);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/oversikt")
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt innlogget saksbehandler.",
        response = OppgaveOversiktDto.class)
    public ResponseEntity<OppgaveOversiktDto> mineOppgaver() {
        String ident = SubjectHandler.getInstance().getUserID();
        List<OppgaveDto> oppgaveDtoListe;
        oppgaveDtoListe = oppgaveService.hentOppgaverMedAnsvarlig(ident);
        OppgaveOversiktDto oversiktDto = new OppgaveOversiktDto();
        List<JournalfoeringsoppgaveDto> journalføring = new ArrayList<>();
        List<BehandlingsoppgaveDto> saksbehandling = new ArrayList<>();

        for (OppgaveDto oppgaveDto : oppgaveDtoListe) {
            if (oppgaveDto instanceof JournalfoeringsoppgaveDto journalfoeringsoppgaveDto) {
                journalføring.add(journalfoeringsoppgaveDto);
            } else if (oppgaveDto instanceof BehandlingsoppgaveDto behandlingsoppgaveDto) {
                saksbehandling.add(behandlingsoppgaveDto);
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
        value = "Søk etter oppgaver knyttet til et fødselsnummer, d-nummer, eller organisasjonsnummer",
        response = no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto.class,
        responseContainer = "List")
    public ResponseEntity<List<no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto>> søkOppgaverMedPersonIdentEllerOrgnr(
        @RequestParam(name = "personIdent", required = false) String personIdent,
        @RequestParam(name = "orgnr", required = false) String orgnr) {
        if (StringUtils.isEmpty(personIdent) && StringUtils.isEmpty(orgnr)) {
            throw new FunksjonellException("Finner ingen søkekriteria. API støtter personIdent(fnr eller dnr) og orgnr");
        }
        if (StringUtils.isNotEmpty(personIdent) && StringUtils.isNotEmpty(orgnr)) {
            throw new FunksjonellException("Fant både personIdent og orgnr. API støtter kun én.");
        }
        try {
            var oppgaveliste = StringUtils.isNotEmpty(personIdent)
                ? oppgaveSoek.finnBehandlingsoppgaverMedPersonIdent(personIdent)
                : oppgaveSoek.finnBehandlingsoppgaverMedOrgnr(orgnr);
            return ResponseEntity.ok(oppgaveliste.stream().map(no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto::av).toList());
        } catch (IkkeFunnetException e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
