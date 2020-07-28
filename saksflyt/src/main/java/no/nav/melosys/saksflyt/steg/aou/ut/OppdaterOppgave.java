package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AnmodningOmUnntakOppdaterOppgave")
public class OppdaterOppgave implements StegBehandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OppdaterOppgave.class);
    private static final String ANMODNING_OM_UNNTAK_SENDT = "Anmodning om unntak er sendt utenlandsk trygdemyndighet.";

    private final OppgaveService oppgaveService;

    @Autowired
    public OppdaterOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_OPPDATER_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        LOGGER.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        LocalDate frist = LocalDate.from(prosessinstans.getBehandling().getDokumentasjonSvarfristDato().atZone(ZoneId.systemDefault()).toLocalDate());

        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(saksnummer);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .beskrivelse(ANMODNING_OM_UNNTAK_SENDT)
            .fristFerdigstillelse(oppgave.getFristFerdigstillelse().isBefore(frist) ? frist : null)
            .build();

        oppgaveService.oppdaterOppgave(oppgave.getOppgaveId(), oppgaveOppdatering);

        LOGGER.info("Oppdatert oppgave {} med beskrivelse, og frist som samsvarer med behandlingsfristen", oppgave.getOppgaveId());
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
