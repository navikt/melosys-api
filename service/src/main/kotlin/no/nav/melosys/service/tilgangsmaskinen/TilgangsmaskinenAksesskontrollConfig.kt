package no.nav.melosys.service.tilgangsmaskinen

import io.getunleash.Unleash
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.AksesskontrollImpl
import no.nav.melosys.service.tilgang.BrukertilgangKontroll
import no.nav.melosys.service.tilgang.RedigerbarKontroll
import no.nav.melosys.sikkerhet.logging.AuditLogger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Konfigurerer tilgangskontroll basert på feature toggle
 *
 * Toggle PÅ (AKSESSKONTROLL_TILGANGSMASKINEN=true) -> TilgangsmaskinenAksesskontroll
 * Toggle AV (default) -> AksesskontrollImpl (ABAC)
 */
@Configuration
class TilgangsmaskinenAksesskontrollConfig {

    companion object {
        private val log = LoggerFactory.getLogger(TilgangsmaskinenAksesskontrollConfig::class.java)
    }

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

        val erTilgangsmaskinenAktivert = unleash.isEnabled(ToggleName.AKSESSKONTROLL_TILGANGSMASKINEN)

        return if (erTilgangsmaskinenAktivert) {
            log.info("Feature toggle AKSESSKONTROLL_TILGANGSMASKINEN er aktivert - bruker TilgangsmaskinenAksesskontroll")
            TilgangsmaskinenAksesskontroll(
                auditLogger = auditLogger,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                tilgangsmaskinenService = tilgangsmaskinenService,
                redigerbarKontroll = redigerbarKontroll,
                oppgaveService = oppgaveService
            )
        } else {
            log.info("Feature toggle AKSESSKONTROLL_TILGANGSMASKINEN er ikke aktivert - bruker AksesskontrollImpl (ABAC)")
            AksesskontrollImpl(
                auditLogger,
                fagsakService,
                behandlingService,
                brukertilgangKontroll,
                redigerbarKontroll,
                oppgaveService
            )
        }
    }
}
