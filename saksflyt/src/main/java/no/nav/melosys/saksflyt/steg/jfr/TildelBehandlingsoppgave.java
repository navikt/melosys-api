package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);

        log.info("Henter behandlingsoppgave for fagsak {}", saksnummer);
        Optional<Oppgave> oppgave = gsakFasade.finnOppgaveMedSaksnummer(saksnummer);

        if (oppgave.isPresent()) {
            String behandlingsoppgaveId = oppgave.get().getOppgaveId();
            log.info("Tildeler behandlingsoppgave {} til saksbehandler {}", behandlingsoppgaveId, saksbehandler);
            gsakFasade.tildelOppgave(behandlingsoppgaveId, saksbehandler);
        } else {
            String feilmelding = "Finner ingen behandlingsoppgaver for fagsak " + saksnummer;
            log.error(feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Prosessinstans {} har tildelt behandlingsoppgave for fagsak {} til saksbehandler {}",
            prosessinstans.getId(), saksnummer, saksbehandler);
    }
}
