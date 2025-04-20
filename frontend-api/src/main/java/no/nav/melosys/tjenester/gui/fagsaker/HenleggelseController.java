package no.nav.melosys.tjenester.gui.fagsaker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.service.sak.HenleggelseService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.HenleggelseDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Tags({
    @Tag(name = "fagsaker"),
    @Tag(name = "henleggelse"),
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class HenleggelseController {
    private final Aksesskontroll aksesskontroll;
    private final HenleggelseService henleggelseService;

    public HenleggelseController(Aksesskontroll aksesskontroll, HenleggelseService henleggelseService) {
        this.aksesskontroll = aksesskontroll;
        this.henleggelseService = henleggelseService;
    }

    @PostMapping("/fagsaker/{saksnr}/henlegg")
    @Operation(summary = "Henlegger en fagsak. Avslutter kun behandling uten endring av saksstatus dersom behandlingtype er NY_VURDERING.")
    public ResponseEntity<Void> henleggFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody HenleggelseDto henleggelseDto) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        henleggelseService.henleggFagsakEllerBehandling(saksnummer, henleggelseDto.begrunnelseKode(), henleggelseDto.fritekst());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/fagsaker/{behandlingID}/henlegg-som-bortfalt")
    @Operation(summary = "Henlegger behandlingen som bortfalt " +
        "Saken settes til bortfalt dersom det ikke eksisterer andre behandlinger")
    public ResponseEntity<Void> henleggSakSomBortfalt(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriserSkriv(behandlingID);

        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);
        return ResponseEntity.noContent().build();
    }
}
