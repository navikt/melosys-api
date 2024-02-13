package no.nav.melosys.tjenester.gui.ftrl.bestemmelser

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.ftrl.bestemmelse.BestemmelserFraBehandlingstema
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@Api(tags = ["ftrl", "bestemmelser"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class FtrlBestemmelseTjeneste(private val bestemmelserFraBehandlingstema: BestemmelserFraBehandlingstema) {

    @GetMapping("/ftrl/bestemmelser/")
    fun hentBestemmelser(@RequestParam("behandlingstema", required = false) behandlingstema: Behandlingstema?): ResponseEntity<FtrlBestemmelserDto> {
        return ResponseEntity.ok(FtrlBestemmelserDto(bestemmelserFraBehandlingstema.bestemmelserFraBehandlingstema(behandlingstema)))
    }

    data class FtrlBestemmelserDto(val bestemmelser: List<Folketrygdloven_kap2_bestemmelser>)
}
