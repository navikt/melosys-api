package no.nav.melosys.tjenester.gui.fagsaker;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.JournalpostInfoDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Tags({
    @Tag(name = "dokumenter"),
    @Tag(name = "fagsaker")
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentoversiktController {
    private final DokumentHentingService dokumentHentingService;

    public DokumentoversiktController(DokumentHentingService dokumentHentingService) {
        this.dokumentHentingService = dokumentHentingService;
    }

    @GetMapping("fagsaker/{saksnummer}/dokumenter")
    @Operation(summary = "Henter alle dokumenter knyttet til en fagsak")
    public ResponseEntity<List<JournalpostInfoDto>> hentDokumenter(@PathVariable("saksnummer") String saksnummer) {
        List<JournalpostInfoDto> dokumentListe = dokumentHentingService.hentJournalposter(saksnummer)
            .stream()
            .map(JournalpostInfoDto::av)
            .sorted(Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())))
            .toList();
        return ResponseEntity.ok(dokumentListe);
    }
}
