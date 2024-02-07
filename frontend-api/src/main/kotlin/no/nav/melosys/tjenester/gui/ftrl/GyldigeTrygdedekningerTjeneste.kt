package no.nav.melosys.tjenester.gui.ftrl

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/ftrl/gyldige-trygdedekninger")
@Api(tags = ["ftrl/gyldige-trygdedekninger"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class GyldigeTrygdedekningerTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val gyldigeTrygdedekningerService: GyldigeTrygdedekningerService
) {

    @GetMapping("/{behandlingID}")
    fun hentGyldigeTrygdedekninger(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<List<Trygdedekninger>> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(gyldigeTrygdedekningerService.hentTrygdedekninger(behandlingID))
    }
}
