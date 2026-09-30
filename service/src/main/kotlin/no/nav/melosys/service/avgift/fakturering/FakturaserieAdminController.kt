package no.nav.melosys.service.avgift.fakturering

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@Protected
@RestController
@Tags(
    Tag(name = "fakturaserie"),
    Tag(name = "admin")
)
@RequestMapping("/admin/fakturaserier")
class FakturaserieAdminController(
    private val behandlingsresultatService: BehandlingsresultatService
) {

    @GetMapping("/{fakturaserieReferanse}/saksnummer")
    @Operation(
        summary = "Finn saksnummer for en fakturaseriereferanse fra faktureringskomponenten",
        description = "Returnerer 404 hvis ingen behandling har referansen."
    )
    fun hentSaksnummer(@PathVariable fakturaserieReferanse: String): ResponseEntity<SaksnummerDto> {
        log.info("Admin: Henter saksnummer for fakturaserieReferanse $fakturaserieReferanse")
        val saksnumre = behandlingsresultatService.finnSaksnumreMedFakturaserieReferanse(fakturaserieReferanse)
        if (saksnumre.isEmpty()) {
            throw IkkeFunnetException("Fant ingen sak med fakturaserieReferanse $fakturaserieReferanse")
        }
        return ResponseEntity.ok(SaksnummerDto(saksnumre.first()))
    }

    data class SaksnummerDto(val saksnummer: String)
}
