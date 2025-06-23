package no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode.dto.HelseutgiftDekkesPeriodeDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/helseutgiftdekkesperiode")
@Tags(
    Tag(name = "helseutgift_dekkes_periode"),
)
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class HelseutgiftDekkesPeriodeController(
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val aksesskontroll: Aksesskontroll,
) {

    @PostMapping("/{behandlingresultatID}")
    fun opprettNyHelseutgiftDekkesPeriode(
        @PathVariable("behandlingresultatID") behandlingresultatID: Long,
        @RequestBody helseutgiftDekkesPeriodeDto: HelseutgiftDekkesPeriodeDto
    ): ResponseEntity<Void> {
        aksesskontroll.autoriserSkriv(behandlingresultatID)

        helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(
            behandlingresultatID,
            helseutgiftDekkesPeriodeDto.fomDato,
            helseutgiftDekkesPeriodeDto.tomDato,
            helseutgiftDekkesPeriodeDto.bostedsland
        )

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{behandlingresultatID}")
    fun hentHelseutgiftDekkesPeriode(
        @PathVariable("behandlingresultatID") behandlingresultatID: Long
    ): ResponseEntity<HelseutgiftDekkesPeriodeDto> {
        aksesskontroll.autoriser(behandlingresultatID)
        return ResponseEntity.ok(
            HelseutgiftDekkesPeriodeDto.av(
                helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(
                    behandlingresultatID
                )
            )
        )
    }
}
