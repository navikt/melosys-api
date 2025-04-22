package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.service.avgift.TrygdeavgiftService
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
@Tags(
    Tag(name = "fagsaker"),
    Tag(name = "trygdeavgift")
)

@RequestMapping("/fagsaker/{saksnummer}/trygdeavgift")
class TrygdeavgiftFagsakController(
    private val aksesskontroll: Aksesskontroll,
    private val trygdeavgiftService: TrygdeavgiftService
) {
    @GetMapping("/oppsummering")
    @Operation(summary = "Hent oppsummering på trygdeavgift på fagsaken")
    fun hentTrygdeavgiftOppsummering(@PathVariable("saksnummer") saksnummer: String): ResponseEntity<TrygdeavgiftOppsummering> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            TrygdeavgiftOppsummering(
                trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(
                    saksnummer
                )
            )
        )
    }
}

data class TrygdeavgiftOppsummering(val harBehandlingMedTrygdeavgift: Boolean)
