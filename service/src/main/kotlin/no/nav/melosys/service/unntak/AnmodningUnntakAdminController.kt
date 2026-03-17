package no.nav.melosys.service.unntak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tags(Tag(name = "anmodning-unntak"), Tag(name = "admin"))
@RequestMapping("/admin/anmodning-unntak")
class AnmodningUnntakAdminController(
    val anmodningUnntakService: AnmodningUnntakService,
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/{behandlingID}/fortsett-uten-sed")
    @Operation(
        summary = "Fortsett anmodning om unntak uten SED",
        description = "Oppdaterer anmodning om unntak uten å sende SED. Setter status til ANMODNING_UNNTAK_SENDT og oppdaterer svarfrist."
    )
    fun oppdaterAnmodningOmUnntakUtenSED(@PathVariable behandlingID: Long): ResponseEntity<Unit> {
        log.info("Admin: Oppdaterer anmodning om unntak uten SED for behandling {}", behandlingID)

        anmodningUnntakService.fortsettAnmodningUtenSed(behandlingID)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{behandlingID}/endre-status-til-vurder-dokument")
    @Operation(
        summary = "Endre behandlingsstatus til VURDER_DOKUMENT",
        description = "Endrer behandlingsstatus til VURDER_DOKUMENT når svar på anmodning allerede er mottatt før forrige statusoppdatering, slik at behandlingen låses opp."
    )
    fun endreStatusTilVurderDokument(@PathVariable behandlingID: Long): ResponseEntity<Unit> {
        log.info("Admin: Endrer status til VURDER_DOKUMENT for behandling {}", behandlingID)

        anmodningUnntakService.endreStatusTilVurderDokument(behandlingID)

        return ResponseEntity.noContent().build()
    }
}
