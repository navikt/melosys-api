package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
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

    private final OppgaveFasade oppgaveFasade;
    private final OppgaveService oppgaveService;

    @Autowired
    public TildelBehandlingsoppgave(@Qualifier("system") OppgaveFasade oppgaveFasade, @Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveFasade = oppgaveFasade;
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);

        log.info("Henter behandlingsoppgave for fagsak {}", saksnummer);
        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(saksnummer);
        String behandlingsoppgaveId = oppgave.getOppgaveId();

        log.info("Tildeler behandlingsoppgave {} til saksbehandler {}", behandlingsoppgaveId, saksbehandler);
        oppgaveFasade.tildelOppgave(behandlingsoppgaveId, saksbehandler);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Prosessinstans {} har tildelt behandlingsoppgave for fagsak {} til saksbehandler {}",
            prosessinstans.getId(), saksnummer, saksbehandler);
    }
}
