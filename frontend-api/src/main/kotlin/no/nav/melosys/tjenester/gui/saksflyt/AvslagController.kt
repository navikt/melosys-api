package no.nav.melosys.tjenester.gui.saksflyt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
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
@Tags(
    Tag(name = "saksflyt"),
    Tag(name = "avslag")
)
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AvslagController(private val aksesskontroll: Aksesskontroll, private val avslagService: AvslagService) {

    @PostMapping("{behandlingID}/manglende-opplysninger")
    @Operation(summary = "Avslår behandling pga manglende opplysninger")
    fun avslåPgaManglendeOpplysninger(@PathVariable("behandlingID") behandlingID: Long, @RequestBody avslagDto: AvslagDto): ResponseEntity<Unit> {
        aksesskontroll.autoriserSkriv(behandlingID)

        avslagService.avslåPgaManglendeOpplysninger(behandlingID, avslagDto.fritekst, SubjectHandler.getInstance().getUserID())
        return ResponseEntity.noContent().build()
    }
}


