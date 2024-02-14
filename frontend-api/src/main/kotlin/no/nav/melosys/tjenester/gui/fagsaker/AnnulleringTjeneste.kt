package no.nav.melosys.tjenester.gui.fagsaker

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.service.sak.AnnullerSakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = ["fagsaker"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AnnulleringTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val annullerSakService: AnnullerSakService

) {

    @PostMapping("/{saksnummer}/annullering")
    @ApiOperation(
        value = "Annullerer en sak",
    )
    fun annullerFagsak(
        @PathVariable("saksnummer") saksnummer: String,
    ) {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        annullerSakService.annullerSak(saksnummer)
    }
}
