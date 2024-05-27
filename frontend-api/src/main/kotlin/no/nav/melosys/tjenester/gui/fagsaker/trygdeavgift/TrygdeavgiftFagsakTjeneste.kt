package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
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
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Api(tags = ["fagsaker", "trygdeavgift"])
@RequestMapping("/fagsaker/{saksnummer}/trygdeavgift")
class TrygdeavgiftFagsakTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService
) {
    @GetMapping("/oppsummering")
    @ApiOperation("Hent oppsummering på trygdeavgift på fagsaken")
    fun hentTrygdeavgiftOppsummering(@PathVariable("saksnummer") saksnummer: String): ResponseEntity<TrygdeavgiftOppsummering> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            TrygdeavgiftOppsummering(
                trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(
                    saksnummer
                )
            )
        )
    }
}

data class TrygdeavgiftOppsummering(val harBehandlingMedTrygdeavgift: Boolean)
