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

    @GetMapping("/request/stats")
    public ResponseEntity<Map<String, String>> requestStats() {
        System.out.println(ThreadLocalAccessInfo.requestWithExtrernalCalls());
        return ResponseEntity.ok(ThreadLocalAccessInfo.requestWithExtrernalCalls());
    }

    @GetMapping("/request/warn")
    public ResponseEntity<Map<String, String>> debugWarnFront() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.debugWarnFront);
    }

    @GetMapping("/process/warn")
    public ResponseEntity<Map<String, String>> debugWarnProcess() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.debugWarnProcess);
    }

    @GetMapping("/process/stats")
    public ResponseEntity<Map<String, String>> processStats() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.processesWithExtrernalCalls());
    }

    @GetMapping("/both/stats")
    public ResponseEntity<Map<String, String>> bothStats() {
        return ResponseEntity.ok(ThreadLocalAccessInfo.bothWithExtrernalCalls());
    }

}
