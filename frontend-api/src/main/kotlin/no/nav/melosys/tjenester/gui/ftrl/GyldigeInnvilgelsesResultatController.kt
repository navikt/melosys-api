package no.nav.melosys.tjenester.gui.ftrl

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.ftrl.GyldigeInnvilgelsesResultat
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/ftrl/innvilgelsesresultat")
@Tags(
    Tag(name = "ftrl"),
    Tag(name = "innvilgelsesresultat"),
)
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class GyldigeInnvilgelsesResultatController {

    @GetMapping
    fun hentGyldigeInnvilgelsesResultat(
        @RequestParam("behandlingstype", required = true) behandlingstype: Behandlingstyper
    ): ResponseEntity<List<InnvilgelsesResultat>> {
        return ResponseEntity.ok(GyldigeInnvilgelsesResultat.hentInnvilgelsesResultat(behandlingstype))
    }
}
