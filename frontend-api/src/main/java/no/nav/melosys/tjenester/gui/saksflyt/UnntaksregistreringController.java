package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.service.UnntaksregistreringService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/unntaksregistrering")
@Tags({
    @Tag(name = "saksflyt"),
    @Tag(name = "unntaksregistrering"),
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UnntaksregistreringController {

    private final Aksesskontroll aksesskontroll;
    private final UnntaksregistreringService unntaksregistreringService;

    public UnntaksregistreringController(Aksesskontroll aksesskontroll, UnntaksregistreringService unntaksregistreringService) {
        this.aksesskontroll = aksesskontroll;
        this.unntaksregistreringService = unntaksregistreringService;
    }

    @PostMapping("{behandlingID}")
    public ResponseEntity<Void> registrerUnntakFraMedlemskap(@PathVariable("behandlingID") Long behandlingID) {
        aksesskontroll.autoriserSkriv(behandlingID);

        unntaksregistreringService.registrerUnntakFraMedlemskap(behandlingID);

        return ResponseEntity.noContent().build();
    }
}
