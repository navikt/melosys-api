package no.nav.melosys.saksflyt.steg.oppgave

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

@Component
class TildelBehandlingsoppgave(private val oppgaveService: OppgaveService) : StegBehandler {
    private val log = KotlinLogging.logger {  }
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE

    override fun utfør(prosessinstans: Prosessinstans) {
        val skalTilordnes =
            prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean::class.java, false)
        if (skalTilordnes) {
            val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
            val saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
            log.info("Henter behandlingsoppgave for fagsak $saksnummer")
            val oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
            if (oppgave.isPresent) {
                val behandlingsoppgaveId = oppgave.get().oppgaveId
                oppgaveService.tildelOppgave(behandlingsoppgaveId, saksbehandler)
                log.info("Tildelt behandlingsoppgave $behandlingsoppgaveId for fagsak $saksnummer")
            } else {
                log.warn("Behandlingsoppgave for saksnummer $saksnummer finnes ikke og kan ikke tildeles.")
            }
        }
    }
}
