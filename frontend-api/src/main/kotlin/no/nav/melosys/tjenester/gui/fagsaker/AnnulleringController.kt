package no.nav.melosys.tjenester.gui.fagsaker

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "fagsaker")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AnnulleringController(
    private val aksesskontroll: Aksesskontroll,
    private val annullerSakService: AnnullerSakService

) {

    @PostMapping("/{saksnummer}/annullering")
    @Operation(
        summary = "Annullerer en sak",
    )
    fun annullerFagsak(
        @PathVariable("saksnummer") saksnummer: String,
    ) {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        annullerSakService.annullerSak(saksnummer)
    }
}
