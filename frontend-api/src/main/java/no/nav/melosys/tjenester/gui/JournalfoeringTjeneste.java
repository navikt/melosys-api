package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.tjenester.gui.dto.journalforing.BehandlingsInformasjon;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
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
@RequestMapping("/journalforing")
@Api(tags = {"journalforing"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class JournalfoeringTjeneste {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjeneste.class);

    private final JournalfoeringService journalføringService;
    private final OppgaveService oppgaveService;

    @Autowired
    public JournalfoeringTjeneste(JournalfoeringService journalføringService, OppgaveService oppgaveService) {
        this.journalføringService = journalføringService;
        this.oppgaveService = oppgaveService;
    }

    @GetMapping("{journalpostID}")
    @ApiOperation(value = "Hent journalpost opplysninger.", response = JournalpostDto.class)
    public ResponseEntity<JournalpostDto> hentJournalpostOpplysninger(@PathVariable("journalpostID") String journalpostID) {
        log.debug("Journalpost med ID {} hentes.", journalpostID);
        Journalpost journalpost = journalføringService.hentJournalpost(journalpostID);
        JournalpostDto journalpostDto = JournalpostDto.av(journalpost, journalføringService.finnBrukerIdent(journalpost).orElse(null));

        if (journalpost.mottaksKanalErEessi()) {
            journalføringService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID)
                .ifPresent(behandlingstema -> journalpostDto.setBehandlingsInformasjon(new BehandlingsInformasjon(Sakstyper.EU_EOS, behandlingstema)));
        }
        return ResponseEntity.ok(journalpostDto);
    }

    @PostMapping("opprett")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public ResponseEntity<Void> opprettSakOgJournalfør(@RequestBody JournalfoeringOpprettDto journalføringDto) {
        journalføringService.opprettOgJournalfør(journalføringDto);
        oppgaveService.ferdigstillOppgave(journalføringDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("sed")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public ResponseEntity<Void> journalførSed(@RequestBody JournalfoeringSedDto journalfoeringSedDto) {
        journalføringService.journalførSed(journalfoeringSedDto);
        oppgaveService.ferdigstillOppgave(journalfoeringSedDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("tilordne")
    @ApiOperation(value = "Tilordne sak og journalfør.")
    public ResponseEntity<Void> tilordneSakOgJournalfør(@RequestBody JournalfoeringTilordneDto journalføringDto) {
        journalføringService.tilordneSakOgJournalfør(journalføringDto);
        oppgaveService.ferdigstillOppgave(journalføringDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }
}
