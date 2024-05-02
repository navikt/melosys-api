package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

@Protected
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Api(tags = ["fagsaker", "trygdeavgift"])
@RequestMapping("/fagsaker/{saksnummer}/aarsavregning")
class ÅrsavregningTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val aarsavregningService: AarsavregningService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer
) {
    @GetMapping("/hentTrygdeavgiftForAar/{aar}")
    fun hentDataForAarsavregning(@PathVariable("saksnummer") saksnummer: String,
                                 @PathVariable("aar") år: Int): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSakstilgang(saksnummer)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(aarsavregningService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, år))
        )
    }

    //TODO heller ikke en del av 6570, men kan brukes når vi skal lagre den ferdige årsberegingen.
    @PostMapping("/{behandlingID}/lagreAarsavregningForAar/{aar}")
    fun lagreDataForAarsavregning(@PathVariable("behandlingID") behandlingID: Long,
                                  @RequestBody årsavgiftDto: ÅrsavgiftDto): ResponseEntity<List<TrygdeavgiftsberegningResponse>> {
        return ResponseEntity.ok(
                aarsavregningService.beregnOgLagreAarsavgift(behandlingID, årsavgiftDto) //TODO lag dette etter att datamodellen er klar
        )
    }

    @Unprotected
    @PostMapping("/hentTotalBeloepForPeriode")
    fun lagreDataForAarsavregning(@PathVariable("behandlingID") behandlingID: Long,
                                  @RequestBody årsavgiftDto: BeregnTotalBeløpDto): ResponseEntity<BigDecimal> {
        return ResponseEntity.ok(
            faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(årsavgiftDto)
        )
    }
}
