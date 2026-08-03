package no.nav.melosys.service.sak

import no.nav.melosys.domain.FagsakStatusEndretEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Løpende synk av saksstatus til melosys-skjema-api ved statusendring på fagsak.
 * [FagsakStatusEndretEvent] publiseres sentralt i FagsakService.oppdaterStatus, som alle
 * fagsak-status-mutasjoner går gjennom — også stier uten aktiv behandling (henleggelse som
 * bortfalt, annullering av sak med kun inaktive behandlinger), som behandlings-eventene
 * ikke dekker.
 *
 * Selve synken kjøres som egen prosessinstans (SYNK_SKJEMA_SAKSSTATUS, jf. mønsteret i
 * FaktureringEventListener/OPPDATER_FAKTURAMOTTAKER), bestilt via
 * [SkjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet]: bestillingen committes ATOMISK i
 * samme transaksjon som statusendringen (outbox-semantikk — synken kan ikke tapes ved krasj),
 * mens HTTP-kallet skjer i steget etter commit, med rekjøringsstøtte fra prosessrammeverket.
 *
 * Flere raske statusendringer på samme sak gir flere prosessinstanser — det er OK: mottaket i
 * skjema-api er idempotent og steget leser gjeldende status ved kjøring. (låsReferanse-dedup
 * brukes ikke: OPPDATER_FAKTURAMOTTAKER-mønsteret bruker det heller ikke, og referansetypene
 * er format-validerte for SED/søknad — saksnummer passer ikke uten ny referansetype.)
 */
@Component
class SkjemaSaksstatusEventListener(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) {

    @EventListener
    @Transactional
    fun fagsakStatusEndret(event: FagsakStatusEndretEvent) {
        skjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet(event.saksnummer)
    }
}
