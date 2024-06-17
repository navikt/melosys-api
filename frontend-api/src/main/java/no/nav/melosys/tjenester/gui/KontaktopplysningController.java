package no.nav.melosys.tjenester.gui;

import java.util.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class KontaktopplysningController {
    private final KontaktopplysningService kontaktopplysningService;
    private final Aksesskontroll aksesskontroll;

    public KontaktopplysningController(KontaktopplysningService kontaktopplysningService, Aksesskontroll aksesskontroll) {
        this.kontaktopplysningService = kontaktopplysningService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(
        value = "Henter kontakt orgnummer og person navn for gitt fagsak og orgnummer",
        response = Kontaktopplysning.class)
    public ResponseEntity hentKontaktopplysning(@PathVariable("saksnummer") String saksnummer,
                                                @PathVariable("orgnr") String orgnr) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        Optional<Kontaktopplysning> kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(saksnummer, orgnr);
        return kontaktopplysning.map(opp -> ResponseEntity.ok(KontaktInfoDto.av(opp)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(
        value = "Lagrer/oppdaterer kontakt orgnummer og navn for gitt fagsak og orgnummer",
        response = Kontaktopplysning.class)
    public ResponseEntity lagKontaktopplysning(@PathVariable("saksnummer") String saksnummer,
                                               @PathVariable("orgnr") String orgnr,
                                               @RequestBody KontaktInfoDto kontaktInfoDto) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        Kontaktopplysning kontaktopplysning = kontaktopplysningService.lagEllerOppdaterKontaktopplysning(saksnummer, orgnr,
            kontaktInfoDto.kontaktorgnr(), kontaktInfoDto.kontaktnavn(), kontaktInfoDto.kontakttelefon());
        return ResponseEntity.ok(kontaktopplysning);
    }

    @DeleteMapping("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(value = "Sletter kontaktopplysning på en fagsak med gitt orgnummer")
    public ResponseEntity slettKontaktopplysning(@PathVariable("saksnummer") String saksnummer, @PathVariable("orgnr") String orgnr) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        kontaktopplysningService.slettKontaktopplysning(saksnummer, orgnr);
        return ResponseEntity.noContent().build();
    }
}
