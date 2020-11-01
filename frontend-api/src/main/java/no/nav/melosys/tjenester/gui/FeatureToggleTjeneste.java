package no.nav.melosys.tjenester.gui;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
public class FeatureToggleTjeneste {

    private final Unleash unleash;

    public FeatureToggleTjeneste(Unleash unleash) {
        this.unleash = unleash;
    }

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> hentFeatureToggles(@RequestBody Set<String> features) {
        return ResponseEntity.ok(features.stream().collect(Collectors.toMap(f -> f, unleash::isEnabled)));
    }
}
