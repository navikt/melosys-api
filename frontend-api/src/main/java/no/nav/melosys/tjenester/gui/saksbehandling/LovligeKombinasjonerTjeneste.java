package no.nav.melosys.tjenester.gui.saksbehandling;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksbehandling")
@Api(tags = {"saksbehandling"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovligeKombinasjonerTjeneste {
    private final LovligeKombinasjonerService lovligeKombinasjonerService;

    public LovligeKombinasjonerTjeneste(LovligeKombinasjonerService lovligeKombinasjonerService) {
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
    }

    @GetMapping("/sakstyper/hent-lovlige-kombinasjoner")
    @ApiOperation(value = "Henter alle mulige sakstyper", notes = ("Henter alle mulige sakstyper"))
    public ResponseEntity<Set<Sakstyper>> hentAlleMuligeSakstyper() {
        return ResponseEntity.ok(lovligeKombinasjonerService.hentMuligeSakstyper());
    }

    @GetMapping("/sakstemaer/hent-lovlige-kombinasjoner")
    @ApiOperation(value = "Henter alle mulige sakstemaer basert på sakstypen", notes = ("Henter alle mulige sakstemaer basert på sakstypen"))
    public ResponseEntity<Set<Sakstemaer>> hentAlleMuligeSakstemaer(
        @RequestParam(value = "hovedpart", required = false) Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype) {
        return ResponseEntity.ok(lovligeKombinasjonerService.hentMuligeSakstemaer(hovedpart, sakstype));
    }

    @GetMapping("/behandlingstemaer/hent-lovlige-kombinasjoner")
    @ApiOperation(value = "Henter alle mulige behandlingstemaer basert på sakstype og sakstema", notes = ("Henter alle mulige behandlingstemaer basert på sakstype og sakstema"))
    public ResponseEntity<Set<Behandlingstema>> hentAlleMuligeBehandlingstemaer(
        @RequestParam(value = "hovedpart", required = false) Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "sistBehandlingstema", required = false) Behandlingstema sistBehandlingstema
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerService.hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema));
    }

    @GetMapping("/behandlingstyper/hent-lovlige-kombinasjoner")
    @ApiOperation(value = "Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema", notes = ("Henter alle mulige behandlingstyper basert på sakstype, sakstema og behandlingstema"))
    public ResponseEntity<Set<Behandlingstyper>> hentAlleMuligeBehandlingstyper(
        @RequestParam("hovedpart") Aktoersroller hovedpart,
        @RequestParam("sakstype") Sakstyper sakstype,
        @RequestParam("sakstema") Sakstemaer sakstema,
        @RequestParam(value = "behandlingstema", required = false) Behandlingstema behandlingstema,
        @RequestParam(value = "sisteBehandlingsID", required = false) Long sisteBehandlingsID
    ) {
        return ResponseEntity.ok(lovligeKombinasjonerService.hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, sisteBehandlingsID));
    }
}
