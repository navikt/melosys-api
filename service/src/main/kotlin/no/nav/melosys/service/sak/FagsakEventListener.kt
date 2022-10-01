package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class FagsakEventListener(
    private val oppgaveService: OppgaveService
) {

    @EventListener
    fun fagsakEndret(fagsakEndretAvSaksbehandler: FagsakEndretAvSaksbehandler) {
        ThreadLocalAccessInfo.executeProcess("fagsakEndret") {
            val fagsak = fagsakEndretAvSaksbehandler.fagsak
            val oppgave: Optional<Oppgave> =
                oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
            oppgave.ifPresentOrElse({
                val behandling = fagsak.hentAktivBehandling()
                val behandlingstype = behandling.type
                val behandlingstema = behandling.tema
                log.info(
                    "Oppdaterer oppgave {} med sakstype {} og sakstema {}", it.oppgaveId, fagsak.type, fagsak.tema
                )
                oppgaveService.oppdaterOppgave(
                    it.oppgaveId, fagsak.type, fagsak.tema, behandlingstema, behandlingstype
                )
            }, {
                log.info("Fagsak ${fagsak.saksnummer} ble endret uten aktiv oppgave")
                oppgaveService.opprettOppgaveForSak(fagsak.saksnummer)
            })
        }
    }
}
