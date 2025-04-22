package no.nav.melosys.tjenester.gui.fagsaker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.UtpekDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker/{saksnummer}/utpek")
@Tags({
    @Tag(name = "fagsaker"),
    @Tag(name = "utpeking")
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtpekingController {
    private final Aksesskontroll aksesskontroll;
    private final FagsakService fagsakService;
    private final UtpekingService utpekingService;

    public UtpekingController(Aksesskontroll aksesskontroll, FagsakService fagsakService,
                              UtpekingService utpekingService) {
        this.aksesskontroll = aksesskontroll;
        this.fagsakService = fagsakService;
        this.utpekingService = utpekingService;
    }

    @PostMapping
    @Operation(summary = "Utpeker lovvalgsland for gitt fagsak")
    public ResponseEntity<Void> utpekLovvalgsland(@PathVariable("saksnummer") String saksnummer,
                                                  @RequestBody UtpekDto utpekDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);

        utpekingService.utpekLovvalgsland(
            fagsak,
            utpekDto.mottakerinstitusjoner(),
            utpekDto.fritekstSed(),
            utpekDto.fritekstBrev()
        );

        return ResponseEntity.noContent().build();
    }
}
