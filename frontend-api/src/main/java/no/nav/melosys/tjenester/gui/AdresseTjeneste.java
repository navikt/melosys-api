package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
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
@RequestMapping("/adresser")
@Api(tags = {"adresser"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AdresseTjeneste {
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public AdresseTjeneste(UtenlandskMyndighetService utenlandskMyndighetService) {
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @GetMapping("/myndigheter/{landkode}")
    @ApiOperation(
        value = "Henter adressen til en gitt utenlandsk myndighet",
        response = UtenlandskMyndighet.class)
    public ResponseEntity<UtenlandskMyndighet> hentMyndighet(@PathVariable("landkode") Landkoder landkode) {
        return ResponseEntity.ok(utenlandskMyndighetService.hentUtenlandskMyndighet(landkode));
    }

    @GetMapping("/myndigheter")
    @ApiOperation(
        value = "Henter adresser til alle utenlandske myndigheter",
        response = UtenlandskMyndighet.class,
        responseContainer = "List")
    public ResponseEntity<List<UtenlandskMyndighet>> hentMyndigheter() {
        return ResponseEntity.ok(utenlandskMyndighetService.hentAlleUtenlandskMyndigheter());
    }
}
