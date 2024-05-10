package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class FagsakEventListener(
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService
) {

    @EventListener
    fun fagsakEndret(fagsakEndretAvSaksbehandler: FagsakEndretAvSaksbehandler) {
        val fagsak = fagsakService.hentFagsak(fagsakEndretAvSaksbehandler.saksnummer)
        val oppgave: Optional<Oppgave> =
            oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        oppgave.ifPresentOrElse({
            val behandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
            log.info(
                "Oppdaterer oppgave ${it.oppgaveId} med sakstype ${fagsak.type} og sakstema ${fagsak.tema}"
            )
            oppgaveService.oppdaterOppgave(
                it.oppgaveId, behandling
            )
        }, {
            log.info("Fagsak ${fagsak.saksnummer} ble endret uten aktiv oppgave")
            oppgaveService.opprettOppgaveForSak(fagsak.saksnummer)
        })
    }
}
