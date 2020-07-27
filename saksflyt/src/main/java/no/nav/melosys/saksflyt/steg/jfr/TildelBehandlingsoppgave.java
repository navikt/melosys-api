package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Optional;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TildelBehandlingsoppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(TildelBehandlingsoppgave.class);

    private final OppgaveService oppgaveService;

    @Autowired
    public TildelBehandlingsoppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);

        log.info("Henter behandlingsoppgave for fagsak {}", saksnummer);
        Optional<Oppgave> oppgave = oppgaveService.finnOppgaveMedFagsaksnummer(saksnummer);
        if (oppgave.isPresent()) {
            String behandlingsoppgaveId = oppgave.get().getOppgaveId();
            oppgaveService.tildelOppgave(behandlingsoppgaveId, saksbehandler);
            log.info("Prosessinstans {} har tildelt behandlingsoppgave {} for fagsak {}",
                prosessinstans.getId(), behandlingsoppgaveId, saksnummer);
        } else {
            log.warn("Behandlingsoppgave for saksnummer {} finnes ikke og kan ikke tildeles.", saksnummer);
        }

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
