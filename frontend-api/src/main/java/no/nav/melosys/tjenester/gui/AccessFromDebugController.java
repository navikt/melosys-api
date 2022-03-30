package no.nav.melosys.tjenester.gui;

import java.util.Map;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping(value = "/access", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccessFromDebugController {

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Integer>> usage() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.debugInfoUsage);
    }

    @GetMapping("/checks")
    public ResponseEntity<Map<String, Integer>> checks() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.debugInfoChecks);
    }
}
