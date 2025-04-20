package no.nav.melosys.tjenester.gui.saksbehandling;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksbehandling")
@Tag(name = "saksbehandling")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovligeKombinasjonerSaksbehandlingController {
    private final LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;

    public LovligeKombinasjonerSaksbehandlingController(LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService) {
        this.lovligeKombinasjonerSaksbehandlingService = lovligeKombinasjonerSaksbehandlingService;
    }

    @GetMapping("/sakstyper/hent-lovlige-kombinasjoner")
    @Operation(summary = "Henter alle mulige sakstyper", description = ("Henter alle mulige sakstyper"))
    public ResponseEntity<Set<Sakstyper>> hentAlleMuligeSakstyper(
        @RequestParam(value = "saksnummer", required = false) String saksnummer
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(saksnummer));
    }

    @GetMapping("/sakstemaer/hent-lovlige-kombinasjoner")
    @Operation(summary = "Henter alle mulige sakstemaer basert på sakstypen", description = ("Henter alle mulige sakstemaer basert på sakstypen"))
    public ResponseEntity<Set<Sakstemaer>> hentAlleMuligeSakstemaer(
        @RequestParam(value = "hovedpart", required = false) Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam(value = "saksnummer", required = false) String saksnummer
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(hovedpart, sakstype, saksnummer));
    }

    @GetMapping("/behandlingstemaer/hent-lovlige-kombinasjoner")
    @Operation(summary = "Henter alle mulige behandlingstemaer basert på sakstype og sakstema", description = ("Henter alle mulige behandlingstemaer basert på sakstype og sakstema"))
    public ResponseEntity<Set<Behandlingstema>> hentAlleMuligeBehandlingstemaer(
        @RequestParam(value = "hovedpart", required = false) Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "aktivBehandlingID", required = false) Long aktivBehandlingID,
        @RequestParam(value = "sistBehandlingstema", required = false) Behandlingstema sistBehandlingstema
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, aktivBehandlingID, sistBehandlingstema));
    }

    @GetMapping("/behandlingstyper/kombinasjoner")
    @Operation(
        summary = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema",
        description = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema")
    )
    public ResponseEntity<Set<Behandlingstyper>> hentAlleMuligeBehandlingstyper(
        @RequestParam("hovedpart") Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "behandlingstema", required = false) Behandlingstema behandlingstema
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema));
    }

    @GetMapping("/{saksnummer}/behandlingstyper/kombinasjoner-for-endring")
    @Operation(
        summary = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema",
        description = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema")
    )
    public ResponseEntity<Set<Behandlingstyper>> hentAlleMuligeBehandlingstyperForEndring(
        @RequestParam("hovedpart") Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "behandlingstema", required = false) Behandlingstema behandlingstema,
        @PathVariable(value = "saksnummer") String saksnummer
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForEndring(hovedpart, sakstype, sakstema, behandlingstema, saksnummer));
    }

    @GetMapping("{saksnummer}/behandlingstyper/kombinasjoner-for-knytt-sak")
    @Operation(summary = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema",
        description = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema"))
    public ResponseEntity<Set<Behandlingstyper>> hentAlleMuligeBehandlingstyperForKnyttTilSak(
        @RequestParam("hovedpart") Aktoersroller hovedpart,
        @PathVariable("saksnummer") String saksnummer,
        @RequestParam(value = "behandlingstema", required = false) Behandlingstema behandlingstema
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(hovedpart, saksnummer, behandlingstema));
    }

    @GetMapping("/behandlingsaarsaktyper/hent-lovlige-kombinasjoner")
    @Operation(summary = "Henter alle mulige behandlingsårsaktyper basert på valgt behandlingstype",
        description = ("Henter alle mulige behandlingsårsaktyper basert på valgt behandlingstype"))
    public ResponseEntity<List<Behandlingsaarsaktyper>> hentAlleMuligeBehandlingsårsaktyper(@RequestParam("behandlingstype") Behandlingstyper behandlingstype) {
        return ResponseEntity.ok(lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingsårsaktyper(behandlingstype));
    }
}
