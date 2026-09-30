package no.nav.melosys.service.avgift.fakturering

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.KonfliktException
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
        description = "Returnerer 404 hvis ingen behandling har referansen, og 409 hvis referansen er knyttet til flere saker."
    )
    fun hentSaksnummer(@PathVariable fakturaserieReferanse: String): ResponseEntity<SaksnummerDto> {
        log.info("Admin: Henter saksnummer for fakturaserieReferanse $fakturaserieReferanse")
        val saksnumre = behandlingsresultatService.finnSaksnumreMedFakturaserieReferanse(fakturaserieReferanse)

        return when (saksnumre.size) {
            0 -> throw IkkeFunnetException("Fant ingen sak med fakturaserieReferanse $fakturaserieReferanse")
            1 -> ResponseEntity.ok(SaksnummerDto(saksnumre.single()))
            else -> throw KonfliktException("FakturaserieReferanse $fakturaserieReferanse er knyttet til flere saker: $saksnumre")
        }
    }

    data class SaksnummerDto(val saksnummer: String)
}
