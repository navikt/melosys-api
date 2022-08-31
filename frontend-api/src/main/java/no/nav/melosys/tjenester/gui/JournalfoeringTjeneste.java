package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.LovligeKombinasjoner;
import no.nav.melosys.tjenester.gui.dto.journalforing.BehandlingsInformasjon;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public JournalfoeringTjeneste(JournalfoeringService journalføringService, OppgaveService oppgaveService) {
        this.journalføringService = journalføringService;
        this.oppgaveService = oppgaveService;
    }

    @GetMapping("{journalpostID}")
    @ApiOperation(value = "Hent journalpost opplysninger", response = JournalpostDto.class)
    public ResponseEntity<JournalpostDto> hentJournalpostOpplysninger(@PathVariable("journalpostID") String journalpostID) {
        log.debug("Journalpost med ID {} hentes.", journalpostID);
        Journalpost journalpost = journalføringService.hentJournalpost(journalpostID);
        JournalpostDto journalpostDto = JournalpostDto.av(journalpost, journalføringService.finnHovedpartIdent(journalpost).orElse(null));

        if (journalpost.mottaksKanalErEessi()) {
            journalføringService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID)
                .ifPresent(behandlingstema -> journalpostDto.setBehandlingsInformasjon(new BehandlingsInformasjon(Sakstyper.EU_EOS, behandlingstema)));
        }
        return ResponseEntity.ok(journalpostDto);
    }

    @PostMapping("opprett")
    @ApiOperation(value = "Journalfør og opprett ny fagsak asynkront")
    public ResponseEntity<Void> journalførOgOpprettSak(@RequestBody JournalfoeringOpprettDto journalføringDto) {
        journalføringService.journalførOgOpprettSak(journalføringDto);
        oppgaveService.ferdigstillOppgave(journalføringDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("sed")
    @ApiOperation(value = "Journalfør SED")
    public ResponseEntity<Void> journalførSed(@RequestBody JournalfoeringSedDto journalfoeringSedDto) {
        journalføringService.journalførSed(journalfoeringSedDto);
        oppgaveService.ferdigstillOppgave(journalfoeringSedDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("knytt")
    @ApiOperation(value = "Journalfør og knytt til fagsak asynkront")
    public ResponseEntity<Void> journalførOgKnyttTilSak(@RequestBody JournalfoeringTilordneDto journalføringDto) {
        journalføringService.journalførOgKnyttTilEksisterendeSak(journalføringDto);
        oppgaveService.ferdigstillOppgave(journalføringDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("nyvurdering")
    @ApiOperation(value = "Journalfør og opprett ny vurdering asynkront")
    public ResponseEntity<Void> journalførOgOpprettNyVurdering(@RequestBody JournalfoeringTilordneDto journalføringDto) {
        journalføringService.journalførOgOpprettNyVurdering(journalføringDto);
        oppgaveService.ferdigstillOppgave(journalføringDto.getOppgaveID());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sakstyper")
    @ApiOperation(value = "Henter alle mulige sakstyper", notes = ("Henter alle mulige sakstyper"))
    public ResponseEntity<Set<Sakstyper>> hentAlleMuligeSakstyper() {
        return ResponseEntity.ok(LovligeKombinasjoner.hentAlleMuligeSakstyper());
    }

    @GetMapping("/sakstemaer/{hovedpart}/{sakstype}")
    @ApiOperation(value = "Henter alle mulige sakstemaer basert på sakstypen", notes = ("Henter alle mulige sakstemaer basert på sakstypen"))
    public ResponseEntity<Set<Sakstemaer>> hentAlleMuligeSakstemaer(@PathVariable("hovedpart") Aktoersroller hovedpartRolle, @PathVariable("sakstype") Sakstyper sakstype) {
        return ResponseEntity.ok(LovligeKombinasjoner.hentAlleSakstemaer(hovedpartRolle, sakstype));
    }

    @GetMapping("/behandlingstemaer/{hovedpart}/{sakstype}/{sakstema}")
    @ApiOperation(value = "Henter alle mulige behandlingstemaer basert på sakstype og sakstema", notes = ("Henter alle mulige behandlingstemaer basert på sakstype og sakstema"))
    public ResponseEntity<Set<Behandlingstema>> hentAllemuligeBehandlingstemaer(
        @PathVariable("hovedpart") Aktoersroller hovedpart,
        @PathVariable("sakstype") Sakstyper sakstype,
        @PathVariable("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "sistBehandlingstema", required = false) Behandlingstema sistBehandlingstema
    ) {
        return ResponseEntity.ok(LovligeKombinasjoner.hentAlleMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema));
    }

    @GetMapping("/behandlingstyper/{hovedpart}/{sakstype}/{sakstema}")
    @ApiOperation(value = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema", notes = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema"))
    public ResponseEntity<Set<Behandlingstyper>> hentAllemuligeBehandlingstyper(
        @PathVariable("hovedpart") Aktoersroller hovedpart,
        @PathVariable("sakstype") Sakstyper sakstype,
        @PathVariable("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "behandlingstema", required = false) Behandlingstema behandlingstema,
        @RequestParam(value = "sistBehandlingstema", required = false) Behandlingstema sistBehandlingstema,
        @RequestParam(value = "sistBehandlingstype", required = false) Behandlingstyper sistBehandlingstype,
        @RequestParam(value = "saksstatus", required = false) Saksstatuser saksstatus
    ) {
        return ResponseEntity.ok(LovligeKombinasjoner.hentAlleMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, sistBehandlingstema, sistBehandlingstype, saksstatus));
    }

}
