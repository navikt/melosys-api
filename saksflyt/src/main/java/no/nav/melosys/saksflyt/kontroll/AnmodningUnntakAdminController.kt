package no.nav.melosys.saksflyt.kontroll

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflyt.steg.sed.SendAnmodningOmUnntak
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Protected
@RestController
@Tags(Tag(name = "anmodning-unntak"), Tag(name = "admin"))
@RequestMapping("/admin/anmodning-unntak")
class AnmodningUnntakAdminController(
    val behandlingService: BehandlingService,
    val anmodningsperiodeService: AnmodningsperiodeService,
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @PostMapping("/{behandlingID}/fortsett-uten-sed")
    fun oppdaterAnmodningOmUnntakUtenSED(@PathVariable behandlingID: Long): ResponseEntity<Unit> {
        log.info("Admin: Oppdaterer anmodning om unntak uten å SED for behandling {}", behandlingID)

        val behandling = behandlingService.hentBehandling(behandlingID)

        val svarFristDato = LocalDateTime.now().plusMonths(SendAnmodningOmUnntak.SVARFRIST_MÅNEDER.toLong())
        behandling.dokumentasjonSvarfristDato = svarFristDato.atZone(SendAnmodningOmUnntak.TIME_ZONE_ID).toInstant()
        behandling.status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
        behandlingService.lagre(behandling)
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(behandling.id)

        return ResponseEntity.noContent().build()

    }
}
