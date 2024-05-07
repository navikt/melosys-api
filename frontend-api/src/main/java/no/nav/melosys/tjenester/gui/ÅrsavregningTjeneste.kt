package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.service.sak.AarsavregningService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

@Protected
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Api(tags = ["Årsavregning", "Trygdeavgift"])
@RequestMapping("/aarsavregning/{saksnummer}")
class ÅrsavregningTjeneste(
    private val aarsavregningService: AarsavregningService,
) {

    @Unprotected
    @PostMapping("/henttotaltrygdeavgiftforperiode")
    fun hentTotalTrygdeavgiftForPeriode(@RequestBody årsavgiftDto: BeregnTotalBeløpDto): ResponseEntity<BigDecimal> {
        return ResponseEntity.ok(
            aarsavregningService.hentTotalTrygdeavgiftForPeriode(årsavgiftDto)
        )
    }
}
