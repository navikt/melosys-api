package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import io.swagger.annotations.Api
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Api(tags = ["fagsaker", "trygdeavgift"])
@RequestMapping("/fagsaker/{saksnummer}/aarsavregning")
class ÅrsavregningTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService
) {
    @GetMapping("/hentTrygdeavgiftForAar/{aar}")
    fun hentDataForAarsavregning(@PathVariable("saksnummer") saksnummer: String,
                                 @PathVariable("aar") år: Int): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftOppsummeringService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, år))
        )
    }

    @PostMapping("/lagreAarsavregningForAar/{aar}")
    fun lagreDataForAarsavregning(@PathVariable("saksnummer") saksnummer: String): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftOppsummeringService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, år))
        )
    }
}
