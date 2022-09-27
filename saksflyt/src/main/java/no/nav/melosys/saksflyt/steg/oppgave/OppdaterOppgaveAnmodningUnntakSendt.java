package no.nav.melosys.saksflyt.steg.oppgave;

import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OppdaterOppgaveAnmodningUnntakSendt implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterOppgaveAnmodningUnntakSendt.class);
    private static final String ANMODNING_OM_UNNTAK_SENDT = "Anmodning om unntak er sendt utenlandsk trygdemyndighet.";

    private final OppgaveService oppgaveService;

    public OppdaterOppgaveAnmodningUnntakSendt(OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        LocalDate frist = LocalDate.from(prosessinstans.getBehandling().getDokumentasjonSvarfristDato().atZone(ZoneId.systemDefault()).toLocalDate());

        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        Oppgave oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .beskrivelse(ANMODNING_OM_UNNTAK_SENDT)
            .fristFerdigstillelse(oppgave.getFristFerdigstillelse().isBefore(frist) ? frist : null)
            .build();

        oppgaveService.oppdaterOppgave(oppgave.getOppgaveId(), oppgaveOppdatering);

        log.info("Oppdatert oppgave {} med beskrivelse, og frist som samsvarer med behandlingsfristen", oppgave.getOppgaveId());
    }
}
