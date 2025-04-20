package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.MottaksdatoDto;
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
@RequestMapping("/behandlinger")
@Tag(name = "behandlingsaarsak")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsaarsakController {

    private final Aksesskontroll aksesskontroll;
    private final UtledMottaksdato utledMottaksdato;
    private final BehandlingService behandlingService;

    public BehandlingsaarsakController(Aksesskontroll aksesskontroll, UtledMottaksdato utledMottaksdato, BehandlingService behandlingService) {
        this.aksesskontroll = aksesskontroll;
        this.utledMottaksdato = utledMottaksdato;
        this.behandlingService = behandlingService;
    }

    @GetMapping("/{behandlingID}/aarsak/mottaksdato")
    @Operation(summary = "Henter mottaksdato fra behandlingsårsak, eller bruk eksisterende logikk for å finne alternativ mottaksdato")
    public ResponseEntity<MottaksdatoDto> hentMottaksdato(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        var behandling = behandlingService.hentBehandling(behandlingID);
        return ResponseEntity.ok(new MottaksdatoDto(utledMottaksdato.getMottaksdato(behandling)));
    }
}
