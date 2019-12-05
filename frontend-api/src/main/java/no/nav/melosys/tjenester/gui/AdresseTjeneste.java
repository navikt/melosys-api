package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@Api(tags = {"adresser"})
@RestController("/adresser")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AdresseTjeneste extends RestTjeneste {

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepo;

    public AdresseTjeneste(UtenlandskMyndighetRepository utenlandskMyndighetRepo) {
        this.utenlandskMyndighetRepo = utenlandskMyndighetRepo;
    }

    @GetMapping("/myndigheter/{landkode}")
    @ApiOperation(
        value = "Henter adressen til en gitt utenlandsk myndighet",
        response = UtenlandskMyndighet.class)
    public ResponseEntity hentMyndighet(@PathVariable("landkode") Landkoder landkode) {
        Optional<UtenlandskMyndighet> utenlandskMyndighet = utenlandskMyndighetRepo.findByLandkode(landkode);
        return utenlandskMyndighet.map(ResponseEntity::ok).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/myndigheter")
    @ApiOperation(
        value = "Henter adresser til alle utenlandske myndigheter",
        response = UtenlandskMyndighet.class,
        responseContainer = "List")
    public ResponseEntity hentMyndigheter() {
        List<UtenlandskMyndighet> utenlandskeMyndigheter = utenlandskMyndighetRepo.findAll();
        return ResponseEntity.ok(utenlandskeMyndigheter);
    }
}