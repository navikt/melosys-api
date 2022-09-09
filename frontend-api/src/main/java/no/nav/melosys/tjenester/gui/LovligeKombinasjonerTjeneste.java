package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.lovligeKombinasjoner.LovligeKombinasjoner;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/lovligeKombinasjoner")
@Api(tags = {"lovligeKombinasjoner"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovligeKombinasjonerTjeneste {
    private static final Logger log = LoggerFactory.getLogger(LovligeKombinasjonerTjeneste.class);

    private final JournalfoeringService journalføringService;

    public LovligeKombinasjonerTjeneste(JournalfoeringService journalføringService) {
        this.journalføringService = journalføringService;
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
    public ResponseEntity<Set<Behandlingstema>> hentAlleMuligeBehandlingstemaer(
        @PathVariable("hovedpart") Aktoersroller hovedpart,
        @PathVariable("sakstype") Sakstyper sakstype,
        @PathVariable("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "sistBehandlingstema", required = false) Behandlingstema sistBehandlingstema
    ) {
        return ResponseEntity.ok(LovligeKombinasjoner.hentAlleMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema));
    }

    @GetMapping("/behandlingstyper/{hovedpart}/{sakstype}/{sakstema}")
    @ApiOperation(value = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema", notes = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema"))
    public ResponseEntity<Set<Behandlingstyper>> hentAlleMuligeBehandlingstyper(
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
