package no.nav.melosys.tjenester.gui.saksflyt

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.service.sak.AvslagService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.tjenester.gui.dto.AvslagDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/saksflyt/avslag")
@Api(tags = ["saksflyt", "avslag"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AvslagTjeneste(private val aksesskontroll: Aksesskontroll, private val avslagService: AvslagService) {

    @PostMapping("{behandlingID}/manglende-opplysninger")
    @ApiOperation(value = "Avslår behandling pga manglende opplysninger")
    fun avslåPgaManglendeOpplysninger(@PathVariable("behandlingID") behandlingID: Long, @RequestBody avslagDto: AvslagDto): ResponseEntity<Unit> {
        aksesskontroll.autoriserSkriv(behandlingID)

        avslagService.avslåPgaManglendeOpplysninger(behandlingID, avslagDto.fritekst, SubjectHandler.getInstance().getUserID())
        return ResponseEntity.noContent().build()
    }
}


