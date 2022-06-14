package no.nav.melosys.tjenester.gui.kontroll;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.security.token.support.core.api.Unprotected;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Unprotected // TODO: Change to Protected!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
@RestController
@RequestMapping("/kontroll")
@Api(tags = "kontroll")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class GodkjennUnntaksperiodeController {

    private final Aksesskontroll aksesskontroll;

    public GodkjennUnntaksperiodeController(Aksesskontroll aksesskontroll) {
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/godkjenn-unntaksperiode")
    public ResponseEntity<Void> harPeriodeOver2ÅrOgEnDag(@RequestBody RequestDto requestDto) {
        aksesskontroll.autoriser(requestDto.behandlingID(), Aksesstype.LES);
        
        return ResponseEntity.noContent().build();
    }

    record RequestDto(@Valid @NotNull Long behandlingID) {
    }
}
