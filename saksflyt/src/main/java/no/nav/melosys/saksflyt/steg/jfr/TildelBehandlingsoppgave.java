package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TildelBehandlingsoppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(TildelBehandlingsoppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public TildelBehandlingsoppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
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
        Oppgave oppgave = gsakFasade.hentOppgaveMedSaksnummer(saksnummer);
        String behandlingsoppgaveId = oppgave.getOppgaveId();

        log.info("Tildeler behandlingsoppgave {} til saksbehandler {}", behandlingsoppgaveId, saksbehandler);
        gsakFasade.tildelOppgave(behandlingsoppgaveId, saksbehandler);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Prosessinstans {} har tildelt behandlingsoppgave for fagsak {} til saksbehandler {}",
            prosessinstans.getId(), saksnummer, saksbehandler);
    }
}
