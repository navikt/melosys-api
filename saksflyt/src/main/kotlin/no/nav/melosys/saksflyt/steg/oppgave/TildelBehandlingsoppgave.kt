package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TildelBehandlingsoppgave(private val oppgaveService: OppgaveService) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val skalTilordnes =
            prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean::class.java, java.lang.Boolean.FALSE)
        if (skalTilordnes) {
            val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
            val saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
            log.info("Henter behandlingsoppgave for fagsak {}", saksnummer)
            val oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
            if (oppgave.isPresent) {
                val behandlingsoppgaveId = oppgave.get().oppgaveId
                oppgaveService.tildelOppgave(behandlingsoppgaveId, saksbehandler)
                log.info("Tildelt behandlingsoppgave {} for fagsak {}", behandlingsoppgaveId, saksnummer)
            } else {
                log.warn("Behandlingsoppgave for saksnummer {} finnes ikke og kan ikke tildeles.", saksnummer)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TildelBehandlingsoppgave::class.java)
    }
}
