package no.nav.melosys.tjenester.gui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.melosys.domain.tekstblokk.Tekstblokk
import no.nav.melosys.service.tekstblokk.TekstblokkService
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkDto
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkRequestDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/tekstblokker")
@Tag(name = "admin-tekstblokker")
class TekstblokkAdminController(
    private val tekstblokkService: TekstblokkService,
) {

    @PostMapping("/bulk")
    @Operation(summary = "Oppretter mange tekstblokker eller brevmaler i én transaksjon")
    fun opprettBulk(@Valid @RequestBody requests: List<TekstblokkRequestDto>): ResponseEntity<List<TekstblokkDto>> {
        val inputs = requests.map { TekstblokkService.Input(it.tittel, it.innhold, it.type, it.tags) }
        val opprettet = tekstblokkService.opprettBulk(inputs)
        return ResponseEntity.ok(opprettet.map(::tilDto))
    }

    private fun tilDto(t: Tekstblokk): TekstblokkDto = TekstblokkDto(
        id = t.id!!,
        tittel = t.tittel,
        innhold = t.innhold,
        type = t.type,
        tags = t.tags.sorted(),
        registrertDato = t.registrertDato,
        registrertAv = t.registrertAv,
        endretDato = t.endretDato,
        endretAv = t.endretAv,
    )
}
