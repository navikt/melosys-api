package no.nav.melosys.tjenester.gui.ftrl

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/ftrl/trygdedekninger")
@Api(tags = ["ftrl", "trygdedekninger"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class GyldigeTrygdedekningerTjeneste(
    private val gyldigeTrygdedekningerService: GyldigeTrygdedekningerService
) {

    @GetMapping()
    fun hentGyldigeTrygdedekninger(
        @RequestParam("behandlingstema", required = true) behandlingstema: Behandlingstema
    ): ResponseEntity<List<Trygdedekninger>> {
        return ResponseEntity.ok(gyldigeTrygdedekningerService.hentTrygdedekninger(behandlingstema))
    }
}
