package no.nav.melosys.tjenester.gui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import no.nav.melosys.service.tekstblokk.TekstblokkService
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkDto
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkRequestDto
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val MAKS_BULK = 500

@RestController
@RequestMapping("/admin/brev/tekstblokker")
@Tag(name = "admin-tekstblokker")
class TekstblokkAdminController(
    private val tekstblokkService: TekstblokkService,
) {

    @PostMapping("/bulk")
    @Operation(summary = "Oppretter mange tekstblokker eller brevmaler i én transaksjon")
    fun opprettBulk(
        @Valid @RequestBody @Size(max = MAKS_BULK) requests: List<TekstblokkRequestDto>,
    ): ResponseEntity<List<TekstblokkDto>> {
        val opprettet = tekstblokkService.opprettBulk(requests.map { it.tilInput() })
        return ResponseEntity.ok(opprettet.map(TekstblokkDto::av))
    }
}
