package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component

/**
 * Pakker behandling av én prosessinstans som en [PrioritertSaksflytTask] og sender den til
 * `saksflytThreadPoolTaskExecutor`. Bruker `execute(Runnable)` (ikke `submit(...)`, som ville pakket den
 * i en ikke-`Comparable` `FutureTask`), slik at køen kan prioritere HØY/NORMAL foran LAV (batch).
 * Effektiv prioritet hentes fra prosessinstansens [ProsessType][no.nav.melosys.saksflytapi.domain.ProsessType].
 */
// kotlin logger
private val log = KotlinLogging.logger { }

@Component
class ProsessinstansDispatcher(
    @param:Qualifier("saksflytThreadPoolTaskExecutor")
    private val saksflytThreadPoolTaskExecutor: ThreadPoolTaskExecutor,
    private val prosessinstansBehandler: ProsessinstansBehandler,
) {

    fun dispatch(prosessinstans: Prosessinstans) {
        val prioritet = prosessinstans.hentPrioritet()
        log.info("Legger prosessinstans {} (prioritet {}) på saksflyt-køen", prosessinstans.id, prioritet)
        saksflytThreadPoolTaskExecutor.execute(
            PrioritertSaksflytTask(
                prosessinstansId = requireNotNull(prosessinstans.id) { "Prosessinstans må ha id før dispatch" },
                prioritet = prioritet,
                registrertDato = prosessinstans.registrertDato,
            ) {
                prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)
            }
        )
    }
}
