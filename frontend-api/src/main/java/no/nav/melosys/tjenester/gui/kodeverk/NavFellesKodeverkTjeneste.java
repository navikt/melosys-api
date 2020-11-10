package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.security.token.support.core.api.Protected;

@Protected
@RestController
@RequestMapping("/kodeverk/nav-felles")
@Api(tags = { "kodeverk/nav-felles"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class NavFellesKodeverkTjeneste {

    private static final Logger log = LoggerFactory.getLogger(NavFellesKodeverkTjeneste.class);
    private final KodeverkService kodeverkService;

    @Autowired
    public NavFellesKodeverkTjeneste(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    @GetMapping("{kodeverkNavn}")
    @ApiOperation("Henter kodeverk fra felles kodeverk")
    public ResponseEntity<List<KodeDto>> hentKodeverk(@PathVariable("kodeverkNavn") FellesKodeverk kodeverkNavn) {
        log.info("Henter kodeverket {} fra felles kodeverk.", kodeverkNavn);
        return ResponseEntity.ok().body(kodeverkService.hentGyldigeKoderForKodeverk(kodeverkNavn));
    }
}
