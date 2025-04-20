package no.nav.melosys.tjenester.gui.avklartefakta;

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.service.avklartefakta.*;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Deprecated
@Protected
@RestController
@RequestMapping("/avklartefakta")
@Tag(name = "avklartefakta")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaController_DEPRECATED {

    private final AvklartefaktaService avklartefaktaService;

    private final Aksesskontroll aksesskontroll;

    public AvklartefaktaController_DEPRECATED( AvklartefaktaService avklartefaktaService, Aksesskontroll aksesskontroll ) {
        this.avklartefaktaService = avklartefaktaService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}")
    @Operation(summary = "Henter avklartefakta for en gitt behandling")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @PostMapping("{behandlingID}")
    @Operation(summary = "Lagre avklartefakta")
    public Set<AvklartefaktaDto> lagreAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                    @RequestBody Set<AvklartefaktaDto> avklartefaktaDtoer) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }
}
