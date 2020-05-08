package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Unprotected
@RestController
@RequestMapping("/logger")
@Api(tags = {"frontend-logger"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class FrontendLoggingTjeneste {

    private static final Logger log = LoggerFactory.getLogger("no.nav.melosys.frontendlogg");

    @PostMapping("/trace")
    @ApiOperation(value = "Logger trace-melding.", notes = ("Logger trace-melding."))
    public ResponseEntity frontendTraceLogging(@RequestBody String loggMelding) {
        log.trace(loggMelding);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/debug")
    @ApiOperation(value = "Logger debug-melding.", notes = ("Logger debug-melding."))
    public ResponseEntity frontendDebugLogging(@RequestBody String loggMelding) {
        log.debug(loggMelding);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/info")
    @ApiOperation(value = "Logger info-melding.", notes = ("Logger info-melding."))
    public ResponseEntity frontendInfoLogging(@RequestBody String loggMelding) {
        log.info(loggMelding);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/warn")
    @ApiOperation(value = "Logger warn-melding.", notes = ("Logger warn-melding."))
    public ResponseEntity frontendWarnLogging(@RequestBody String loggMelding) {
        log.warn(loggMelding);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/error")
    @ApiOperation(value = "Logger error-melding.", notes = ("Logger error-melding."))
    public ResponseEntity frontendErrorLogging(@RequestBody String loggMelding) {
        log.error(loggMelding);
        return ResponseEntity.ok().build();
    }

}
