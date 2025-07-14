package no.nav.melosys.service.avgift.satsendring

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
    fun opprettSatsendringsprosesserForÅr(år: Int, dryRun: Boolean, antallFeilFørStopp: Int = 0) {
        runAsSystem {
            log.info("Starter jobb som oppretter satsendringsprosesser for år: $år, dry run: $dryRun, antall feil før stopp: $antallFeilFørStopp")

            jobMonitor.execute(antallFeilFørStopp) {
                // SatsendringFinner logger allerede det som er relevant, så vi trenger ikke å logge her
                val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)
                jobMonitor.stats.satsendringGrunnlag = satsendringInfo

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
                    lagEnkelSatsendringsprosess(it, satsendringInfo.år, dryRun)
                }

                satsendringInfo.behandlingerMedSatsendringOgBerørtAktivBehandling.forEach {
                    if (jobMonitor.shouldStop) {
                        return@execute
                    }
                    lagSatsendringsprosessNårAktivPåfølgendeBehandling(it, satsendringInfo.år, dryRun)
                }
            }
        }
    }

    private fun <T> runAsSystem(processName: String = "opprettSatsendringsprosesserForÅr", block: () -> T): T {
        val processID = UUID.randomUUID()
        log.info("Starter $processName som systemprosess $processID")
        ThreadLocalAccessInfo.beforeExecuteProcess(processID, processName)
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processID)
        }
    }

    private fun lagEnkelSatsendringsprosess(
        behandlingInfo: SatsendringFinner.BehandlingInfo,
        år: Int,
        dryRun: Boolean
    ) {
        runCatching {
            val behandling = behandlingService.hentBehandling(behandlingInfo.behandlingID)

            if (dryRun) {
                log.info("Ville opprettet satsendringsprosess for sak ${behandlingInfo.saksnummer} og behandlingID: ${behandlingInfo.behandlingID}")
                jobMonitor.stats.enkelSatsendringProsesser.add(
                    ProsessInfo("DRY_RUN", behandlingInfo.saksnummer, behandlingInfo.behandlingID)
                )
            } else {
                val uuid = prosessinstansService.opprettSatsendringBehandlingFor(behandling, år)
                log.info("Opprettet satsendringsprosess: $uuid for sak ${behandlingInfo.saksnummer} og behandlingID: ${behandlingInfo.behandlingID}")
                jobMonitor.stats.enkelSatsendringProsesser.add(
                    ProsessInfo(uuid.toString(), behandlingInfo.saksnummer, behandlingInfo.behandlingID)
                )
            }
        }.onFailure { e ->
            log.error(e) { "Kunne ikke opprette satsendring prosess for behandlingID: ${behandlingInfo.behandlingID}" }
            jobMonitor.registerException(e)
            jobMonitor.stats.opprettelsesfeil++
        }
    }

    private fun lagSatsendringsprosessNårAktivPåfølgendeBehandling(
        behandlingInfo: SatsendringFinner.BehandlingInfo,
        år: Int,
        dryRun: Boolean
    ) {
        runCatching {
            val behandling = behandlingService.hentBehandling(behandlingInfo.behandlingID)

            if (dryRun) {
                log.info("Ville opprettet satsendring med ny vurdering prosess for sak ${behandlingInfo.saksnummer} og behandling ID: ${behandlingInfo.behandlingID}")
                jobMonitor.stats.satsendringSamtAktivBehandlingProsesser.add(
                    ProsessInfo("DRY_RUN", behandlingInfo.saksnummer, behandlingInfo.behandlingID)
                )
            } else {
                val uuid = prosessinstansService.opprettSatsendringBehandlingMedTilbakestillingAvAvgift(behandling, år)
                log.info("Opprettet satsendring med ny vurdering prosess: $uuid for sak ${behandlingInfo.saksnummer} og behandling ID: ${behandlingInfo.behandlingID}")
                jobMonitor.stats.satsendringSamtAktivBehandlingProsesser.add(
                    ProsessInfo(uuid.toString(), behandlingInfo.saksnummer, behandlingInfo.behandlingID)
                )
            }
        }.onFailure { e ->
            log.error(e) { "Kunne ikke opprette satsendring med ny vurdering prosess for behandling ID: ${behandlingInfo.behandlingID}" }
            jobMonitor.registerException(e)
            jobMonitor.stats.opprettelsesfeil++
        }
    }

    fun satsendringInfo(): SatsendringFinner.AvgiftSatsendringInfo? = jobMonitor.stats.satsendringInfo()

    fun status(): Map<String, Any?> = jobMonitor.status()
}

class SatsendringBatchStats : JobMonitor.Stats {
    @Volatile
    var satsendringGrunnlag: SatsendringFinner.AvgiftSatsendringInfo? = null
    var enkelSatsendringProsesser: MutableSet<ProsessInfo> = ConcurrentHashMap.newKeySet()
    var satsendringSamtAktivBehandlingProsesser: MutableSet<ProsessInfo> = ConcurrentHashMap.newKeySet()
    @Volatile
    var opprettelsesfeil: Int = 0

    fun satsendringInfo(): SatsendringFinner.AvgiftSatsendringInfo? = satsendringGrunnlag

    override fun reset() {
        satsendringGrunnlag = null
        enkelSatsendringProsesser.clear()
        satsendringSamtAktivBehandlingProsesser.clear()
        opprettelsesfeil = 0
    }

    override fun asMap(): Map<String, Any?> = mapOf(
        "enkelSatsendringProsesser" to enkelSatsendringProsesser.toList(),
        "totalEnkelSatsendringProsesser" to enkelSatsendringProsesser.size,
        "satsendringSamtAktivBehandlingProsesser" to satsendringSamtAktivBehandlingProsesser.toList(),
        "totalSatsendringSamtAktivBehandlingProsesser" to satsendringSamtAktivBehandlingProsesser.size,
        "totalFeilet" to opprettelsesfeil
    )
}

data class ProsessInfo(
    val prosessUUID: String,
    val saksnummer: String,
    val opprinneligBehandling: Long
)
