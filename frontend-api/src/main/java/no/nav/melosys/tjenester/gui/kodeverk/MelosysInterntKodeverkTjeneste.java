package no.nav.melosys.tjenester.gui.kodeverk;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.HELSEDEL;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.HELSE_OG_PENSJONSDEL;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.PENSJONSDEL;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.security.token.support.core.api.Protected;

@Protected
@RestController
@RequestMapping("/kodeverk/melosys-internt")
@Api(tags = { "kodeverk/melosys-internt"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MelosysInterntKodeverkTjeneste {

    private static final Logger log = LoggerFactory.getLogger(MelosysInterntKodeverkTjeneste.class);

    @GetMapping("/folketrygden")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    public ResponseEntity<List<Trygdedekninger>> hentTrygdedekningerForFTRL() {
        log.info("Henter oder fra internt kodeverk til saksbehandling av folketrygden-saker.");
        return ResponseEntity.ok().body(List.of(HELSEDEL, HELSEDEL_MED_SYKE_OG_FORELDREPENGER, PENSJONSDEL, HELSE_OG_PENSJONSDEL, HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER));
    }
}
