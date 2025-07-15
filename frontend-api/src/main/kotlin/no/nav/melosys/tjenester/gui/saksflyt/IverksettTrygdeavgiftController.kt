package no.nav.melosys.tjenester.gui.saksflyt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.service.avgift.IverksettTrygdeavgiftService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.IverksettTrygdeavgiftPensjonistDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/saksflyt/iverksett/trygdeavgift")
@Tags(
    Tag(name = "saksflyt"),
    Tag(name = "eos"),
    Tag(name = "iverksett"),
    Tag(name = "trygdeavgift")
)
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class IverksettTrygdeavgiftController(private val aksesskontroll: Aksesskontroll, private val iverksettTrygdeavgiftService: IverksettTrygdeavgiftService) {

    @PostMapping("{behandlingID}/pensjonist")
    @Operation(summary = "Iverksett trygdeavgift for pensjonister i EØS")
    fun iverksettTrygdeavgiftEosPensjonist(@PathVariable("behandlingID") behandlingID: Long, @RequestBody iverksettTrygdeavgiftPensjonistDto: IverksettTrygdeavgiftPensjonistDto): ResponseEntity<Unit> {
        aksesskontroll.autoriserSkriv(behandlingID)

        iverksettTrygdeavgiftService.opprettProsessIverksettTrygdeavgiftPensjonist(behandlingID, iverksettTrygdeavgiftPensjonistDto.behandlingsresultatTypeKode, iverksettTrygdeavgiftPensjonistDto.vedtakstype)
        return ResponseEntity.noContent().build()
    }
}


