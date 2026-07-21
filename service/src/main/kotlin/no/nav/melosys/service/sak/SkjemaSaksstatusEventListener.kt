package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Løpende synk av saksstatus til melosys-skjema-api ved statusendring på behandling.
 * [BehandlingEndretStatusEvent] publiseres sentralt i BehandlingService (endreStatus + avslutt),
 * så én listener dekker alle prosesser som endrer status. Fagsakens status er allerede oppdatert
 * når eventet publiseres (f.eks. FagsakService.avsluttFagsakOgBehandling setter saksstatus før
 * behandlingen avsluttes).
 *
 * Feil her skal ALDRI velte saksbehandlingsflyten — de logges, og den idempotente massesynken
 * (admin-endepunktet /admin/skjema-saksstatus/synk) er sikkerhetsnettet for tapte oppdateringer.
 */
@Component
class SkjemaSaksstatusEventListener(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) {

    @EventListener
    fun behandlingEndretStatus(event: BehandlingEndretStatusEvent) {
        try {
            skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(event.behandling.fagsak)
        } catch (e: Exception) {
            log.error(e) {
                "Kunne ikke synkronisere saksstatus til skjema-api for sak ${event.behandling.fagsak.saksnummer}. " +
                    "Kan rettes med massesynk (/admin/skjema-saksstatus/synk)."
            }
        }
    }
}
