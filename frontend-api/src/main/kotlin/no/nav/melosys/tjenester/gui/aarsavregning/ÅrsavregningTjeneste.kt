package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.service.sak.ÅrsavregningService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Protected
@RestController
@Api(tags = ["Årsavregning", "Trygdeavgift"])
@RequestMapping("/aarsavregning")
class ÅrsavregningTjeneste(
    private val årsavregningService: ÅrsavregningService,
) {
    //TODO må tilpasses i MELOSYS-6528.
    @PostMapping("/beregntotaltrygdeavgiftforperiode")
    fun hentTotalTrygdeavgiftForPeriode(@RequestBody beregnTotalBeløpDto: BeregnTotalBeløpDto): ResponseEntity<BigDecimal> {
        return ResponseEntity.ok(
            årsavregningService.beregnTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto)
        )
    }
}
