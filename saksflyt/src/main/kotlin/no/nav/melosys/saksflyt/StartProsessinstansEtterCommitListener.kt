package no.nav.melosys.saksflyt

import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.StartProsessinstansEtterCommitEvent
import no.nav.melosys.service.behandling.BehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listener som oppretter prosessinstanser ETTER at transaksjonen som publiserte eventet har committet.
 *
 * Dette sikrer at all data som ble lagret i den opprinnelige transaksjonen er synlig i databasen
 * før prosessen starter og eventuelt leser denne dataen.
 *
 * Flyten blir:
 * 1. Transaksjon A: Oppdater data + publiser StartProsessinstansEtterCommitEvent
 * 2. Transaksjon A committer → data er nå synlig i DB
 * 3. Denne listener kjører (AFTER_COMMIT) → oppretter prosessinstans i ny transaksjon
 * 4. Prosessinstans-transaksjon committer
 * 5. ProsessinstansOpprettetListener (AFTER_COMMIT) → starter async prosess
 * 6. Async prosess leser fra DB → ser garantert oppdatert data
 */
@Component
class StartProsessinstansEtterCommitListener(
    private val prosessinstansService: ProsessinstansService,
    private val behandlingService: BehandlingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun opprettProsessinstansEtterCommit(event: StartProsessinstansEtterCommitEvent) {
        log.info(
            "Oppretter prosessinstans {} for behandling {} etter commit",
            event.prosessType,
            event.behandlingId
        )

        val behandling = behandlingService.hentBehandling(event.behandlingId)

        when (event) {
            is StartProsessinstansEtterCommitEvent.IverksettVedtakFtrl -> {
                prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(
                    behandling,
                    event.vedtakRequest,
                    event.saksstatus
                )
            }

            is StartProsessinstansEtterCommitEvent.IverksettVedtakTrygdeavtale -> {
                prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling)
            }

            is StartProsessinstansEtterCommitEvent.IverksettVedtakAarsavregning -> {
                prosessinstansService.opprettProsessinstansIverksettVedtakÅrsavregning(behandling)
            }
        }
    }
}
