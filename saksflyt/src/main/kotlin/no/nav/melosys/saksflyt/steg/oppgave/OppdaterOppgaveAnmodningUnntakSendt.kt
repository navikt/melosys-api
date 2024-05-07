package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
class OppdaterOppgaveAnmodningUnntakSendt(private val oppgaveService: OppgaveService) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val frist = LocalDate.from(
            prosessinstans.behandling.dokumentasjonSvarfristDato.atZone(ZoneId.systemDefault()).toLocalDate()
        )
        val saksnummer = prosessinstans.behandling.fagsak.saksnummer
        val oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
        val oppgaveOppdatering = OppgaveOppdatering.builder()
            .beskrivelse(ANMODNING_OM_UNNTAK_SENDT)
            .fristFerdigstillelse(if (oppgave.fristFerdigstillelse.isBefore(frist)) frist else null)
            .build()
        oppgaveService.oppdaterOppgave(oppgave.oppgaveId, oppgaveOppdatering)
        log.info(
            "Oppdatert oppgave {} med beskrivelse, og frist som samsvarer med behandlingsfristen",
            oppgave.oppgaveId
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppdaterOppgaveAnmodningUnntakSendt::class.java)
        private const val ANMODNING_OM_UNNTAK_SENDT = "Anmodning om unntak er sendt utenlandsk trygdemyndighet."
    }
}
