package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Tag(name = "fagsaker")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AktoerController {
    private final Aksesskontroll aksesskontroll;
    private final AktoerService aktoerService;
    private final FagsakService fagsakService;

    public AktoerController(Aksesskontroll aksesskontroll,
                          AktoerService aktoerService,
                          FagsakService fagsakService) {
        this.aksesskontroll = aksesskontroll;
        this.aktoerService = aktoerService;
        this.fagsakService = fagsakService;
    }

    @GetMapping("/{saksnummer}/aktoerer")
    @Operation(
        summary = "Henter aktører knyttet til et gitt saksnummer."
    )
    public List<AktoerDto> hentAktoerer(@PathVariable("saksnummer") String saksnummer,
                                        @RequestParam(value = "rolleKode", required = false) String rolleKode) {

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        var rolle = StringUtils.isNotEmpty(rolleKode) ? Aktoersroller.valueOf(rolleKode) : null;

        List<Aktoer> aktører = aktoerService.hentfagsakAktører(fagsak, rolle);

        return aktører.stream().map(AktoerDto::tilDto).toList();
    }

    @PostMapping("/{saksnummer}/aktoerer")
    @Operation(
        summary = "Lagrer/oppdaterer aktør informasjon for et gitt saksnummer."
    )
    public ResponseEntity<AktoerDto> lagAktoerer(@PathVariable("saksnummer") String saksnummer,
                                                 @RequestBody AktoerDto aktoerDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);
        Long databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);
        aktoerDto.setDatabaseID(databaseId);
        return ResponseEntity.ok(aktoerDto);
    }

    @DeleteMapping("/aktoerer/{databaseID}")
    @Operation(
        summary = "Sletter aktøren med en gitt database-id."
    )
    public ResponseEntity<Void> slettAktoer(@PathVariable("databaseID") long databaseID) {
        aktoerService.slettAktoer(databaseID);
        return ResponseEntity.noContent().build();
    }
}
