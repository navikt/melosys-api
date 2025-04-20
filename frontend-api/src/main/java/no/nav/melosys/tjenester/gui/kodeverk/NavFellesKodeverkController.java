package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kodeverk/nav-felles")
@Tag(name = "kodeverk/nav-felles")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class NavFellesKodeverkController {

    private static final Logger log = LoggerFactory.getLogger(NavFellesKodeverkController.class);
    private final KodeverkService kodeverkService;

    public NavFellesKodeverkController(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    @GetMapping("{kodeverkNavn}")
    @Operation(summary = "Henter kodeverk fra felles kodeverk")
    public ResponseEntity<List<KodeDto>> hentKodeverk(@PathVariable("kodeverkNavn") FellesKodeverk kodeverkNavn) {
        log.info("Henter kodeverket {} fra felles kodeverk.", kodeverkNavn);
        return ResponseEntity.ok().body(
            KodeDto.tilKodeDto(kodeverkService.hentGyldigeKoderForKodeverk(kodeverkNavn))
        );
    }
}
