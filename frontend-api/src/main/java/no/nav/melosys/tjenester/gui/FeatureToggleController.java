package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import io.getunleash.Unleash;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/featuretoggle")
@Api(tags = {"featuretoggle"})
public class FeatureToggleController {

    private final Unleash unleash;

    public FeatureToggleController(Unleash unleash) {
        this.unleash = unleash;
    }

    @GetMapping
    @ApiOperation(value = "Returnerer om oppgitte feature-toggles er aktiv")
    public ResponseEntity<Map<String, Boolean>> hentFeatureToggles(@RequestParam Collection<String> features) {
        return ResponseEntity.ok(features.stream().collect(Collectors.toMap(f -> f, unleash::isEnabled)));
    }
}
