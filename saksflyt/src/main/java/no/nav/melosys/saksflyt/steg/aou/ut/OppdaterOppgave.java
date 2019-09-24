package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("AnmodningOmUnntakOppdaterOppgave")
public class OppdaterOppgave extends AbstraktStegBehandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OppdaterOppgave.class);
    private static final String ANMODNING_OM_UNNTAK_SENDT = "Anmodning om unntak er sendt utenlandsk trygdemyndighet.";

    private final GsakFasade gsakFasade;

    @Autowired
    public OppdaterOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_OPPDATER_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        LOGGER.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        LocalDate frist = LocalDate.from(prosessinstans.getBehandling().getDokumentasjonSvarfristDato().atZone(ZoneId.systemDefault()).toLocalDate());

        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        Oppgave oppgave = gsakFasade.hentOppgaveMedSaksnummer(saksnummer);
        
        Oppgave.Builder builder = new Oppgave.Builder(oppgave);

        if (oppgave.getFristFerdigstillelse().isBefore(frist)) {
            builder.setFristFerdigstillelse(frist);
        }
        builder.setBeskrivelse(StringUtils.isEmpty(oppgave.getBeskrivelse()) ? ANMODNING_OM_UNNTAK_SENDT : oppgave.getBeskrivelse() + System.lineSeparator() + ANMODNING_OM_UNNTAK_SENDT);
        
        gsakFasade.oppdaterOppgave(builder.build());

        LOGGER.info("Oppdatert oppgave {} med beskrivelse, og frist som samsvarer med behandlingsfristen", oppgave.getOppgaveId());
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
