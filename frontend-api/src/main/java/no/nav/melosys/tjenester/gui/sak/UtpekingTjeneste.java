package no.nav.melosys.tjenester.gui.sak;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = {"fagsaker", "utpeking"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtpekingTjeneste {
    private final Aksesskontroll aksesskontroll;
    private final FagsakService fagsakService;
    private final UtpekingService utpekingService;

    public UtpekingTjeneste(Aksesskontroll aksesskontroll, FagsakService fagsakService,
                            UtpekingService utpekingService) {
        this.aksesskontroll = aksesskontroll;
        this.fagsakService = fagsakService;
        this.utpekingService = utpekingService;
    }

    @PostMapping
    @ApiOperation(value = "Utpeker lovvalgsland for gitt fagsak")
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
