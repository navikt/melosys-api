package no.nav.melosys.tjenester.gui.ftrl.bestemmelser

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.ftrl.bestemmelse.FtrlBestemmelser
import no.nav.melosys.service.ftrl.medlemskapsperiode.PliktigeMedlemskapsbestemmelser
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
class FtrlBestemmelseTjeneste(private val ftrlBestemmelser: FtrlBestemmelser) {

    @GetMapping("/ftrl/bestemmelser")
    fun hentBestemmelser(
        @RequestParam("behandlingstema", required = false) behandlingstema: Behandlingstema?,
        @RequestParam("trygdedekning", required = false) trygdedekning: Trygdedekninger?
    ): ResponseEntity<FtrlBestemmelserDto> {
        return ResponseEntity.ok(FtrlBestemmelserDto(ftrlBestemmelser.hentBestemmelser(behandlingstema, trygdedekning)))
    }

    @GetMapping("/ftrl/bestemmelser/pliktige")
    fun hentBestemmelser(): ResponseEntity<FtrlBestemmelserDto> {
        return ResponseEntity.ok(FtrlBestemmelserDto(PliktigeMedlemskapsbestemmelser.bestemmelser))
    }

    data class FtrlBestemmelserDto(val bestemmelser: List<Folketrygdloven_kap2_bestemmelser>)
}
