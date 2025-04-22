package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/featuretoggle")
@Tag(name = "featuretoggle")
public class FeatureToggleController {

    private final Unleash unleash;

    public FeatureToggleController(Unleash unleash) {
        this.unleash = unleash;
    }

    @GetMapping
    @Operation(summary = "Returnerer om oppgitte feature-toggles er aktiv")
    public ResponseEntity<Map<String, Boolean>> hentFeatureToggles(@RequestParam Collection<String> features) {
        return ResponseEntity.ok(features.stream().collect(Collectors.toMap(f -> f, unleash::isEnabled)));
    }
}
