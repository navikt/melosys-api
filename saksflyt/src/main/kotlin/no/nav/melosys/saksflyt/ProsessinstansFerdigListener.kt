package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.LåsReferanseFactory
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansFerdigListener(
    private val prosessinstansRepository: ProsessinstansRepository,
    private val prosessinstansBehandler: ProsessinstansBehandler
) {
    @EventListener
    fun prosessinstansFerdig(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        if (prosessinstansFerdigEvent.låsReferanse == null) {
            log.info("Prosessinstans ${prosessinstansFerdigEvent.uuid} ferdig uten låsreferanse")
            return
        }

        log.info("Prosessinstans ${prosessinstansFerdigEvent.uuid} ferdig, sjekker om neste med låsreferanse:${prosessinstansFerdigEvent.låsReferanse} kan startes")
        if (kanNesteProsessinstansStartes(prosessinstansFerdigEvent)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun kanNesteProsessinstansStartes(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean =
        prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT).filter {
            LåsReferanseFactory.harSammeGruppePrefiks(it.hentLåsReferanse, prosessinstansFerdigEvent.låsReferanse)
        }.apply {
            log.info("Prosessinstans(er) på vent med samme gruppe-prefiks: ${this.map { it.id }}")
        }.isNotEmpty()

    private val Prosessinstans.parentId: UUID?
        get() = getData(ProsessDataKey.PROCESS_PARENT_ID, UUID::class.java)

    private fun ProsessinstansFerdigEvent.finnSibling(): Prosessinstans? =
        prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .filter { it.parentId == parentId }
            .filter { it.låsReferanse == låsReferanse }
            .sortedBy { it.registrertDato } // Ta den eldste først
            .firstOrNull().apply {
                this?.let { log.debug { "Fant sibling ${it.id} til $uuid med parent:$parentId og lås:$låsReferanse" } }
            }

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        val alleISammeGruppePåVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .filter { LåsReferanseFactory.harSammeGruppePrefiks(it.hentLåsReferanse, prosessinstansFerdigEvent.låsReferanse) }
            .sortedBy { it.registrertDato } // Ta den eldste først

        val nesteSomSkalStartes =
            alleISammeGruppePåVent
                .firstOrNull { it.parentId == prosessinstansFerdigEvent.uuid } // ta sub-prosesser først
                ?: prosessinstansFerdigEvent.finnSibling() // ta så sibling-prosesser
                ?: alleISammeGruppePåVent.firstOrNull() // ta hovedprosesser når vi ikke har flere sub/sibling-prosesser på vent

        if (nesteSomSkalStartes != null) {
            oppdaterStatusOgBehandleProsessinstans(nesteSomSkalStartes)
        }
    }

    private fun oppdaterStatusOgBehandleProsessinstans(prosessinstans: Prosessinstans) {
        log.info("Prosessinstans {} med låsreferanse {} startes opp etter å ha vært på vent", prosessinstans.id, prosessinstans.låsReferanse)
        prosessinstans.status = ProsessStatus.KLAR
        prosessinstans.endretDato = LocalDateTime.now()
        prosessinstansRepository.save(prosessinstans)
        prosessinstansBehandler.behandleProsessinstans(prosessinstans)
    }
}

