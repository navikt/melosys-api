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
        val processID = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processID, "AvsluttArt13BehandlingJobb")
        try {
            avsluttBehandlingerMedMidlertidigLovvalgsbeslutning()
        } catch (e: Exception) {
            log.error(e) { "Feil ved kjøring av avsluttBehandlingArt13Jobb" }
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processID)
        }
    }

    private fun avsluttBehandlingerMedMidlertidigLovvalgsbeslutning() {
        log.info { "Starter avsluttBehandlingArt13Jobb" }
        val behandlingIDer = behandlingService.hentBehandlingIderMedStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
        log.info { "Fant ${behandlingIDer.size} behandling(er) med status MIDLERTIDIG_LOVVALGSBESLUTNING" }

        behandlingIDer.forEach { behandlingID ->
            avsluttBehandlingHvisToMndHarPassert(behandlingID)
        }
    }

    private fun avsluttBehandlingHvisToMndHarPassert(behandlingID: Long) {
        runCatching {
            log.info { "Sjekker om behandling $behandlingID skal avsluttes" }
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }.onFailure { e ->
            log.error(e) { "Feil ved avslutting av behandling $behandlingID" }
        }
    }
}

