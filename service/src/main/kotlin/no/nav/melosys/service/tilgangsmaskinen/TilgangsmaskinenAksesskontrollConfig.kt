package no.nav.melosys.service.tilgangsmaskinen

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.*
import no.nav.melosys.sikkerhet.logging.AuditLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional

/**
 * Konfigurerer tilgangskontroll med runtime feature toggle checking
 *
 * RuntimeDelegatingAksesskontroll sjekker feature toggle ved hver kall og delegerer til:
 * - Toggle PÅ (AKSESSKONTROLL_TILGANGSMASKINEN=true) -> TilgangsmaskinenAksesskontroll
 * - Toggle AV (default) -> AksesskontrollImpl (ABAC)
 */

private val log = KotlinLogging.logger { }

@Configuration
class TilgangsmaskinenAksesskontrollConfig {

    @Bean
    @Primary
    fun aksesskontroll(
        unleash: Unleash,
        auditLogger: AuditLogger,
        fagsakService: FagsakService,
        behandlingService: BehandlingService,
        redigerbarKontroll: RedigerbarKontroll,
        oppgaveService: OppgaveService,
        brukertilgangKontroll: BrukertilgangKontroll,
        tilgangsmaskinenService: TilgangsmaskinenService
    ): Aksesskontroll {

        log.info("Konfigurerer runtime-delegating aksesskontroll med feature-toggle")

        val tilgangsmaskinenAksesskontroll = TilgangsmaskinenAksesskontroll(
            auditLogger = auditLogger,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            tilgangsmaskinenService = tilgangsmaskinenService,
            redigerbarKontroll = redigerbarKontroll,
            oppgaveService = oppgaveService
        )

        val abacAksesskontroll = AksesskontrollImpl(
            auditLogger,
            fagsakService,
            behandlingService,
            brukertilgangKontroll,
            redigerbarKontroll,
            oppgaveService
        )

        // Returner runtime-delegating wrapper for å håndtere toggle
        return RuntimeDelegatingAksesskontroll(
            unleash = unleash,
            tilgangsmaskinenAksesskontroll = tilgangsmaskinenAksesskontroll,
            abacAksesskontroll = abacAksesskontroll
        )
    }
}
