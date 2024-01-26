package no.nav.melosys.tjenester.gui.fagsaker;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.sak.HenleggFagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.HenleggelseDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"fagsaker", "henleggelse"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class HenleggelseTjeneste {
    private final Aksesskontroll aksesskontroll;
    private final HenleggFagsakService henleggFagsakService;

    public HenleggelseTjeneste(Aksesskontroll aksesskontroll, HenleggFagsakService henleggFagsakService) {
        this.aksesskontroll = aksesskontroll;
        this.henleggFagsakService = henleggFagsakService;
    }

    @PostMapping("/fagsaker/{saksnr}/henlegg")
    @ApiOperation(value = "Henlegger en fagsak. Avslutter kun behandling uten endring av saksstatus dersom behandlingtype er NY_VURDERING.")
    public ResponseEntity<Void> henleggFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody HenleggelseDto henleggelseDto) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, henleggelseDto.begrunnelseKode, henleggelseDto.fritekst);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/fagsaker/{saksnr}/henlegg-som-bortfalt", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Henlegger en fagsak i Melosys som bortfalt, fordi den ikke skal behandles i Melosys. " +
        "Henlegger kun den aktive behandlingen uten endring av saksstatus dersom behandlingtype er NY_VURDERING.")
    public ResponseEntity<Void> henleggSakSomBortfalt(@PathVariable("saksnr") String saksnummer) {
        aksesskontroll.autoriserSakstilgang(saksnummer);

        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(saksnummer);
        return ResponseEntity.noContent().build();
    }
}
