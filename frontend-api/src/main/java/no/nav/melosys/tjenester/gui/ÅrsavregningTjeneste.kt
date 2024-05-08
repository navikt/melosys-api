package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.ÅrsavregningDto
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
@Api(tags = ["trygdeavgift"])
@RequestMapping("/fagsaker/{saksnummer}/aarsavregning")
class ÅrsavregningTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val aarsavregningService: AarsavregningService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer
) {
    @GetMapping("/henttrygdeavgiftforaar/{aar}")
    fun hentDataForÅrsavregning(@PathVariable("saksnummer") behandlingsId: Long,
                                @PathVariable("aar") år: Int): ResponseEntity<ÅrsavregningDto> {
        aksesskontroll.autoriser(behandlingsId)
        return ResponseEntity.ok(
            aarsavregningService.hentPerioderForAarsavregningForBehandling(behandlingsId, år)
        )
    }
}
