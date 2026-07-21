package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.FagsakStatusEndretEvent
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Løpende synk av saksstatus til melosys-skjema-api ved statusendring på fagsak.
 * [FagsakStatusEndretEvent] publiseres sentralt i FagsakService.oppdaterStatus, som alle
 * fagsak-status-mutasjoner går gjennom — også stier uten aktiv behandling (henleggelse som
 * bortfalt, annullering av sak med kun inaktive behandlinger), som behandlings-eventene
 * ikke dekker.
 *
 * Selve synken kjøres som egen prosessinstans (SYNK_SKJEMA_SAKSSTATUS, jf. mønsteret i
 * FaktureringEventListener/OPPDATER_FAKTURAMOTTAKER): bestillingen committes ATOMISK i samme
 * transaksjon som statusendringen (outbox-semantikk — synken kan ikke tapes ved krasj), mens
 * HTTP-kallet skjer i steget etter commit, med rekjøringsstøtte fra prosessrammeverket.
 *
 * Flere raske statusendringer på samme sak gir flere prosessinstanser — det er OK: mottaket i
 * skjema-api er idempotent og steget leser gjeldende status ved kjøring. (låsReferanse-dedup
 * brukes ikke: OPPDATER_FAKTURAMOTTAKER-mønsteret bruker det heller ikke, og referansetypene
 * er format-validerte for SED/søknad — saksnummer passer ikke uten ny referansetype.)
 */
@Component
class SkjemaSaksstatusEventListener(
    private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    private val prosessinstansService: ProsessinstansService
) {

    @EventListener
    @Transactional
    fun fagsakStatusEndret(event: FagsakStatusEndretEvent) {
        val saksnummer = event.fagsak.saksnummer
        if (skjemaSakMappingRepository.finnSkjemaIderForSaksnummer(saksnummer).isEmpty()) {
            return
        }

        log.info { "Bestiller synk av saksstatus til skjema-api for sak $saksnummer" }
        prosessinstansService.opprettProsessinstansSynkSkjemaSaksstatus(saksnummer)
    }
}
