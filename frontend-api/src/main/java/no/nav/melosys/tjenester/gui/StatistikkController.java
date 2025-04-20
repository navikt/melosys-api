package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.service.statistikk.StatistikkService;
import no.nav.melosys.tjenester.gui.dto.statistikk.StatistikkDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/statistikk")
@Tag(name = "statistikk")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class StatistikkController {
    private final StatistikkService statistikkService;

    public StatistikkController(StatistikkService statistikkService) {
        this.statistikkService = statistikkService;
    }

    @GetMapping
    @Operation(summary = "Saksbehandlingsstatistikk")
    public ResponseEntity<StatistikkDto> hentStatistikk() {
        return ResponseEntity.ok(new StatistikkDto(statistikkService.hentUtildelteOppgaverStatistikk()));
    }
}
