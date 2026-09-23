package no.nav.melosys.service.sak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@Protected
@RestController
@Tags(
    Tag(name = "fagsak"),
    Tag(name = "admin")
)
@RequestMapping("/admin/fagsaker")
class EndreSakstypeTilEuEosAdminController(
    private val endreSakstypeTilEuEosAdminService: EndreSakstypeTilEuEosAdminService
) {

    @PostMapping("/endre-sakstype-til-eu-eos")
    @Operation(
        summary = "Endre sakstype til EU/EØS for saker med ugyldig behandlingstema (MELOSYS-8309)",
        description = "Finner selv saker fra digital søknad med sakstype TRYGDEAVTALE/FTRL der behandlingstemaet på " +
            "aktiv behandling ikke er gyldig for sakstypen, og endrer sakstype til EU/EØS. Endringen går via samme " +
            "tjeneste som «Endre sak» i GUI, og behandlingstema, -type og -status beholdes. " +
            "Med dryRun=true (default) vises bare hva som ville blitt endret."
    )
    fun endreSakstypeTilEuEøs(
        @RequestParam(defaultValue = "true") dryRun: Boolean
    ): ResponseEntity<List<EndreSakstypeResultat>> {
        log.info { "Admin endrer sakstype til EU/EØS for saker med ugyldig behandlingstema (dryRun=$dryRun)" }
        return ResponseEntity.ok(endreSakstypeTilEuEosAdminService.endreTilEuEøs(dryRun))
    }
}
