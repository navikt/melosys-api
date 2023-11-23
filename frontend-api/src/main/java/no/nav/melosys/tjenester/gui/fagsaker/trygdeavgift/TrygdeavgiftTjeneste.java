package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.fagsaker.TrygdeavgiftOppsummering;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker/{saksnummer}/trygdeavgift")
@Api(tags = {"fagsaker", "trygdeavgift"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TrygdeavgiftTjeneste {
    private final Aksesskontroll aksesskontroll;
    private final TrygdeavgiftOppsummeringService trygdeavgiftOppsummeringService;

    public TrygdeavgiftTjeneste(Aksesskontroll aksesskontroll, TrygdeavgiftOppsummeringService trygdeavgiftOppsummeringService) {
        this.aksesskontroll = aksesskontroll;
        this.trygdeavgiftOppsummeringService = trygdeavgiftOppsummeringService;
    }


    @GetMapping("/oppsummering")
    @ApiOperation("Hent oppsummering på trygdeavgift på fagsaken")
    public ResponseEntity<TrygdeavgiftOppsummering> hentTrygdeavgiftOppsummering(@PathVariable("saksnummer") String saksnummer) {
        aksesskontroll.autoriserSakstilgang(saksnummer);

        return ResponseEntity.ok(new TrygdeavgiftOppsummering(trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(saksnummer)));
    }
}
