package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.service.sak.ÅrsavregningService
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Api(tags = ["Årsavregning", "Trygdeavgift"])
@RequestMapping("/aarsavregning")
class ÅrsavregningTjeneste(
    private val årsavregningService: ÅrsavregningService,
) {
    @PostMapping("/beregntotaltrygdeavgiftforperiode")
    fun hentTotalTrygdeavgiftForPeriode(@RequestBody beregnTotalBeløpDto: BeregnTotalBeløpDto): ResponseEntity<BigDecimal> {
        return ResponseEntity.ok(
            årsavregningService.beregnTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto)
        )
    }
}
