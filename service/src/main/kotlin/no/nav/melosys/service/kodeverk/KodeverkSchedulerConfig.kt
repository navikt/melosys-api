package no.nav.melosys.service.kodeverk

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled

private val log = KotlinLogging.logger { }

@Configuration
@ConditionalOnProperty(
    name = ["melosys.kodeverk.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true // Standard aktivert for produksjonssikkerhet
)
class KodeverkSchedulerConfig(
    private val kodeverkService: KodeverkService
) {

    @EventListener
    fun onApplicationReady(event: ApplicationReadyEvent) {
        log.info("KodeverkScheduler aktivert - forhåndslaster kodeverk ved oppstart")
        ThreadLocalAccessInfo.executeProcess("kodeverkPreload") {
            kodeverkService.lastKodeverk()
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    @SchedulerLock(name = "KodeverkSchedulerJobb", lockAtLeastFor = "10m")
    fun scheduledKodeverkRefresh() {
        log.info("Kjører planlagt oppdatering av kodeverk")
        ThreadLocalAccessInfo.executeProcess("kodeverkScheduler") {
            kodeverkService.lastKodeverk()
        }
    }
}
