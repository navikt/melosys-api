package no.nav.melosys.saksflyt.steg.oppgave;

import java.util.Optional;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TildelBehandlingsoppgave implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(TildelBehandlingsoppgave.class);

    private final OppgaveService oppgaveService;

    public TildelBehandlingsoppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        boolean skalTilordnes = prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class, Boolean.FALSE);

        if (skalTilordnes) {
            String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
            String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);

            log.info("Henter behandlingsoppgave for fagsak {}", saksnummer);
            Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(saksnummer);
            if (oppgave.isPresent()) {
                String behandlingsoppgaveId = oppgave.get().getOppgaveId();
                oppgaveService.tildelOppgave(behandlingsoppgaveId, saksbehandler);
                log.info("Tildelt behandlingsoppgave {} for fagsak {}", behandlingsoppgaveId, saksnummer);
            } else {
                log.warn("Behandlingsoppgave for saksnummer {} finnes ikke og kan ikke tildeles.", saksnummer);
            }
        }
    }
}
