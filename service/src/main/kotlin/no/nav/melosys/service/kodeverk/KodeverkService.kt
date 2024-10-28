package no.nav.melosys.service.kodeverk

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.ObjectUtils
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class KodeverkService(private val kodeverkRegister: KodeverkRegister) {
    @EventListener
    fun onApplicationEvent(event: ApplicationReadyEvent?) {
        kodeverkScheduler()
    }

    fun dekod(kodeverk: FellesKodeverk, kode: String?): String {
        return dekod(kodeverk, kode, LocalDate.now())
    }

    private fun dekod(kodeverk: FellesKodeverk, kode: String?, dato: LocalDate): String {
        if (ObjectUtils.isEmpty(kode)) {
            log.warn("Metode dekod kalt for kodeverk {} med kode {}", kodeverk, kode)
            return UKJENT
        }
        val kodeperioder = hentKodeverk(kodeverk.navn).koder[kode]
        return getTermFraKodeverk(kodeverk, kode, dato, kodeperioder)
    }

    fun hentGyldigeKoderForKodeverk(kodeverk: FellesKodeverk): List<Kode> {
        if (ObjectUtils.isEmpty(kodeverk)) {
            log.error("Metode hentGyldigeKoderForKodeverk kalt for kodeverk {}", kodeverk)
            return emptyList()
        }
        val hentetKodeverk = hentKodeverk(kodeverk.navn)
        val gyldigeKoder: MutableList<Kode> = ArrayList()
        val idag = LocalDate.now()
        for ((_, value) in hentetKodeverk.koder) {
            value.stream().filter { kode: Kode -> !kode.gyldigFom.isAfter(idag) && !kode.gyldigTom.isBefore(idag) }
                .findFirst().ifPresent { e: Kode -> gyldigeKoder.add(e) }
        }
        return gyldigeKoder
    }

    private fun hentKodeverk(kodeverkNavn: String): Kodeverk {
        log.debug("Hentet og cachet Kodeverk {}", kodeverkNavn)
        return kodeverkRegister.hentKodeverk(kodeverkNavn)
    }

    fun getTermFraKodeverk(kodeverk: FellesKodeverk, kode: String?): String {
        return getTermFraKodeverk(
            kodeverk,
            kode,
            LocalDate.now(),
            kodeverkRegister.hentKodeverk(kodeverk.navn).koder[kode]
        )
    }

    private fun getTermFraKodeverk(
        kodeverk: FellesKodeverk,
        kode: String?,
        dato: LocalDate,
        kodeperioder: List<Kode>?
    ): String {
        if (kodeperioder == null) {
            log.warn("Fant ikke term for kode {} i kodeverk {}", kode, kodeverk.navn)
            return UKJENT
        }
        // kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
        for (kodeperiode in kodeperioder) {
            if (!kodeperiode.gyldigFom.isAfter(dato) && !kodeperiode.gyldigTom.isBefore(dato)) {
                return kodeperiode.navn
            }
        }
        log.warn("Fant ingen gyldig term for kode {} i kodeverk {}", kode, kodeverk.navn)
        return UKJENT
    }

    @Scheduled(cron = "0 0 6 * * *")
    @SchedulerLock(name = "KodeverkSchedulerJobb", lockAtLeastFor = "10m")
    fun kodeverkScheduler() {
        log.info("Henter alle kodeverk")
        ThreadLocalAccessInfo.executeProcess("kodeverkScheduler") {
            for (kodeverk in FellesKodeverk.values()) {
                hentKodeverk(kodeverk.navn)
            }
        }
    }

    companion object {
        const val UKJENT = "UKJENT"
    }
}
