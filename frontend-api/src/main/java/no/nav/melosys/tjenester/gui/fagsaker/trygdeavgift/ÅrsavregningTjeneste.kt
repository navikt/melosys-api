package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
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
    private val aarsavregningService: AarsavregningService,
) {
    @GetMapping("/hentTrygdeavgiftForAar/{aar}")
    fun hentDataForAarsavregning(@PathVariable("saksnummer") saksnummer: String,
                                 @PathVariable("aar") år: Int): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(aarsavregningService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, år))
        )
    }

    @PostMapping("{behandlingID}/lagreAarsavregningForAar/{aar}")
    fun lagreDataForAarsavregning(@PathVariable("behandlingID") behandlingID: Long,
                                  @RequestBody årsavgiftDto: ÅrsavgiftDto): ResponseEntity<List<TrygdeavgiftsberegningResponse>> {
        return ResponseEntity.ok(
                aarsavregningService.beregnOgLagreAarsavgift(behandlingID, årsavgiftDto) //TODO lag dette etter att datamodellen er klar
        )
    }
}
