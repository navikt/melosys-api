package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
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
    private static Logger log = LoggerFactory.getLogger(JournalfoeringTjeneste.class);

    private JournalfoeringService journalføringService;

    @Autowired
    public JournalfoeringTjeneste(JournalfoeringService journalføringService) {
        this.journalføringService = journalføringService;
    }

    @GetMapping("{journalpostID}")
    @ApiOperation(value = "Hent journalpost opplysninger.", response = JournalpostDto.class)
    public ResponseEntity hentJournalpostOpplysninger(@PathVariable("journalpostID") String journalpostID) throws MelosysException {
        log.debug("Journalpost med ID {} hentes.", journalpostID);
        Journalpost journalpost = journalføringService.hentJournalpost(journalpostID);
        JournalpostDto journalpostDto = JournalpostDto.av(journalpost);

        if (journalpost.mottaksKanalErEessi()) {
            journalføringService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID)
                .ifPresent(behandlingstema -> journalpostDto.setBehandlingsInformasjon(new BehandlingsInformasjon(Sakstyper.EU_EOS, behandlingstema)));
        }
        return ResponseEntity.ok(journalpostDto);
    }

    @PostMapping("opprett")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public ResponseEntity opprettSakOgJournalfør(@RequestBody JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        journalføringService.opprettOgJournalfør(journalfoeringDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("sed")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public ResponseEntity journalførSed(@RequestBody JournalfoeringSedDto journalfoeringSedDto) throws MelosysException {
        journalføringService.journalførSed(journalfoeringSedDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("tilordne")
    @ApiOperation(value = "Tilordne sak og journalfør.")
    public ResponseEntity tilordneSakOgJournalfør(@RequestBody JournalfoeringTilordneDto journalfoeringDto) throws MelosysException {
        journalføringService.tilordneSakOgJournalfør(journalfoeringDto);
        return ResponseEntity.noContent().build();
    }
}
