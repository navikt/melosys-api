package no.nav.melosys.service.sak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "admin")
@RequestMapping("/admin/skjema-saksstatus")
class SkjemaSaksstatusAdminController(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) {

    @PostMapping("/synk")
    @Operation(
        summary = "Massesynk av saksstatus til melosys-skjema-api",
        description = "Synkroniserer brukervendt saksstatus (MOTTATT/AVSLUTTET) for alle saker med skjema-sak-mapping " +
            "til melosys-skjema-api, og backfiller samtidig saksnummer på innsendinger som mangler det. " +
            "Med dryRun=true (default) bygges kun rapporten uten kall til skjema-api. " +
            "Idempotent — trygt å kjøre flere ganger."
    )
    fun synkroniserSaksstatus(
        @RequestParam(defaultValue = "true") dryRun: Boolean
    ): ResponseEntity<SkjemaSaksstatusSynkRapport> {
        log.info { "Admin trigger massesynk av saksstatus til skjema-api (dryRun=$dryRun)" }
        return ResponseEntity.ok(skjemaSaksstatusSyncService.massesynk(dryRun))
    }
}
