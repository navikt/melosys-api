package no.nav.melosys.saksflyt.steg.sed.jfr;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettJournalfoeringsOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettJournalfoeringsOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettJournalfoeringsOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_JFR_OPPG;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        Tema tema = avklarTema(prosessinstans);

        String oppgaveID = gsakFasade.opprettJournalføringsOppgave(journalpostID, aktørID, tema);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Journalføringsoppgave opprettet med ID {}", oppgaveID);
    }

    private Tema avklarTema(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        if (harGsakSaksnummer(prosessinstans)) {
            Long gsakSaksnummer = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
            return gsakFasade.hentTemaFraSak(gsakSaksnummer);
        }

        return Tema.MED;
    }

    private boolean harGsakSaksnummer(Prosessinstans prosessinstans) {
        return prosessinstans.getBehandling() != null
            && prosessinstans.getBehandling().getFagsak() != null
            && prosessinstans.getBehandling().getFagsak().getGsakSaksnummer() != null;
    }
}
