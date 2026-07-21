package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.FagsakStatusEndretEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger { }

/**
 * Løpende synk av saksstatus til melosys-skjema-api ved statusendring på fagsak.
 * [FagsakStatusEndretEvent] publiseres sentralt i FagsakService.oppdaterStatus, som alle
 * fagsak-status-mutasjoner går gjennom — også stier uten aktiv behandling (henleggelse som
 * bortfalt, annullering av sak med kun inaktive behandlinger), som behandlings-eventene
 * ikke dekker.
 *
 * Kjøres AFTER_COMMIT slik at HTTP-kallet går utenfor transaksjonen: en rollback etter kallet
 * kan ikke gi permanent feil status i skjema-api, og kallet holder ikke DB-tilkoblingen.
 * fallbackExecution=true håndterer eventer publisert utenfor transaksjon.
 *
 * Feil her skal ALDRI velte saksbehandlingsflyten — de logges, og den idempotente massesynken
 * (admin-endepunktet /admin/skjema-saksstatus/synk) er sikkerhetsnettet for tapte oppdateringer.
 */
@Component
class SkjemaSaksstatusEventListener(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun fagsakStatusEndret(event: FagsakStatusEndretEvent) {
        try {
            skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(event.fagsak)
        } catch (e: Exception) {
            log.error(e) {
                "Kunne ikke synkronisere saksstatus til skjema-api for sak ${event.fagsak.saksnummer}. " +
                    "Kan rettes med massesynk (/admin/skjema-saksstatus/synk)."
            }
        }
    }
}
