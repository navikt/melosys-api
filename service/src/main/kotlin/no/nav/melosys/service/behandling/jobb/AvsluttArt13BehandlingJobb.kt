package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class AvsluttArt13BehandlingJobb(
    private val behandlingService: BehandlingService,
    private val avsluttArt13BehandlingService: AvsluttArt13BehandlingService
) {

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "avsluttBehandlingArt13Jobb", lockAtLeastFor = "10m")
    fun avsluttBehandlingArt13() {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, "AvsluttArt13BehandlingJobb")
        try {
            log.info { "Starter avsluttBehandlingArt13Jobb" }
            val behandlingIder = behandlingService.hentBehandlingIderMedStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
            log.info { "Antall behandlinger med status MIDLERTIDIG_LOVVALGSBESLUTNING: ${behandlingIder.size}" }
            behandlingIder.forEach { avsluttHvis2MndHarPassert(it) }
        } catch (e: Exception) {
            log.error(e) { "Feil ved kjøring av avsluttBehandlingArt13Jobb" }
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    private fun avsluttHvis2MndHarPassert(behandlingID: Long) {
        try {
            log.info { "Starter avsluttBehandlingArt13Jobb for behandling $behandlingID" }
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        } catch (e: Exception) {
            log.error(e) { "Feil ved avslutting av behandling $behandlingID" }
        }
    }
}

