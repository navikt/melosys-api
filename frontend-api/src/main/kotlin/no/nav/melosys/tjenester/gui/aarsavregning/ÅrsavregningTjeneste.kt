package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.service.sak.ÅrsavregningService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Api(tags = ["årsavregning", "trygdeavgift"])
@RequestMapping("/aarsavregninger")
class ÅrsavregningTjeneste(
    private val årsavregningService: ÅrsavregningService,
) {
    @GetMapping("/{avregningID}")
    fun hentAvregning(@PathVariable("avregningID") behandlingID: Long): ResponseEntity<ÅrsavregningDto> {
        return ResponseEntity.ok(
            ÅrsavregningDto(
                aar = 2023
            )
        )
    }
}

data class ÅrsavregningDto(
    val aar: Int,
)
