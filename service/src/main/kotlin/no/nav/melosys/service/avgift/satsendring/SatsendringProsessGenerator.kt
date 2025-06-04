package no.nav.melosys.service.avgift.satsendring

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class SatsendringProsessGenerator(
    private val satsendringFinner: SatsendringFinner,
    private val behandlingService: BehandlingService,
    private val prosessinstansService: ProsessinstansService
) {
    private val jobMonitor = JobMonitor("SatsendringsprosesserBatch", SatsendringBatchStats())

    @Async("taskExecutor")
    @Transactional
    fun opprettSatsendringsprosesserForÅr(år: Int, antallFeilFørStopp: Int = 0) {
        log.info("Starter jobb som oppretter satsendringsprosesser for år: $år")

        jobMonitor.execute(antallFeilFørStopp) {
            val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)

            if (satsendringInfo.behandlingerSomFeilet.isNotEmpty()) {
                // Behandlinger hvor man ikke kan avgjøre om det er en satsendring pga. teknisk feil i SatsendringFinner
                val antallBehandlingerMedFeil = satsendringInfo.behandlingerSomFeilet.size
                jobMonitor.exceptions["SatsendringFinner feiler"] = antallBehandlingerMedFeil
                jobMonitor.errorCount += antallBehandlingerMedFeil
            }

            satsendringInfo.behandlingerMedSatsendring.forEach {
                if (jobMonitor.shouldStop) {
                    return@execute
                }
                lagEnkelSatsendringsprosess(satsendringInfo.år, it)
            }

            satsendringInfo.behandlingerMedSatsendringOgNyVurdering.forEach {
                if (jobMonitor.shouldStop) {
                    return@execute
                }
                lagSatsendringsprosessNårAktivPåfølgendeBehandling(satsendringInfo.år, it)
            }
        }
    }

    private fun lagEnkelSatsendringsprosess(
        år: Int,
        behandlingInfo: SatsendringFinner.BehandlingInfo
    ) {
        runCatching {
            val behandling = behandlingService.hentBehandling(behandlingInfo.behandlingID)
            val uuid = prosessinstansService.opprettSatsendringBehandlingFor(behandling, år)

            log.info("Opprettet satsendringsprosess: $uuid for sak ${behandlingInfo.saksnummer} og behandlingID: ${behandlingInfo.behandlingID}")

            jobMonitor.stats.totalEnkelSatsendring++
        }.onFailure { e ->
            log.error(e) { "Kunne ikke opprette satsendring prosess for behandlingID: ${behandlingInfo.behandlingID}" }
            jobMonitor.registerException(e)
            jobMonitor.stats.opprettelsesfeil++
        }
    }

    private fun lagSatsendringsprosessNårAktivPåfølgendeBehandling(
        år: Int,
        behandlingInfo: SatsendringFinner.BehandlingInfo
    ) {
        runCatching {
            val behandling = behandlingService.hentBehandling(behandlingInfo.behandlingID)
            val uuid = prosessinstansService.opprettSatsendringBehandlingNyVurderingFor(behandling, år)

            log.info("Opprettet satsendring med ny vurdering prosess: $uuid for sak ${behandlingInfo.saksnummer} " +
                    "og behandling ID: ${behandlingInfo.behandlingID}")

            jobMonitor.stats.totalSatsendringSamtAktivBehandling++
        }.onFailure { e ->
            log.error(e) { "Kunne ikke opprette satsendring med ny vurdering prosess for behandling ID: ${behandlingInfo.behandlingID}" }
            jobMonitor.registerException(e)
            jobMonitor.stats.opprettelsesfeil++
        }
    }
}

class SatsendringBatchStats : JobMonitor.Stats {
    var totalEnkelSatsendring: Int = 0
    var totalSatsendringSamtAktivBehandling: Int = 0
    var opprettelsesfeil: Int = 0

    override fun reset() {
        totalEnkelSatsendring = 0
        totalSatsendringSamtAktivBehandling = 0
        opprettelsesfeil = 0
    }

    override fun asMap(): Map<String, Any?> {
        return mapOf(
            "totalEnkelSatsendring" to totalEnkelSatsendring,
            "totalSatsendringSamtAktivBehandling" to totalSatsendringSamtAktivBehandling,
            "totalFeilet" to opprettelsesfeil
        )
    }
}
