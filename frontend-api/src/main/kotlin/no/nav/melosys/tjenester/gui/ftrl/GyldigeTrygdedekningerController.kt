package no.nav.melosys.tjenester.gui.ftrl

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
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
@RequestMapping("/ftrl/trygdedekninger")
@Api(tags = ["ftrl", "trygdedekninger"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class GyldigeTrygdedekningerController(
    private val gyldigeTrygdedekningerService: GyldigeTrygdedekningerService
) {

    @GetMapping
    fun hentGyldigeTrygdedekninger(
        @RequestParam("behandlingstema", required = true) behandlingstema: Behandlingstema,
        @RequestParam("bestemmelse", required = false) bestemmelse: Folketrygdloven_kap2_bestemmelser?
    ): ResponseEntity<List<Trygdedekninger>> {
        return ResponseEntity.ok(gyldigeTrygdedekningerService.hentTrygdedekninger(behandlingstema, bestemmelse))
    }
}
