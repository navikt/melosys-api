package no.nav.melosys.saksflyt.steg.jfr.sed;

import java.time.LocalDate;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.SakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettJournalfoeringsOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettJournalfoeringsOppgave.class);

    private final GsakFasade gsakFasade;
    private final SakService sakService;

    @Autowired
    public OpprettJournalfoeringsOppgave(@Qualifier("system") GsakFasade gsakFasade, SakService sakService) {
        this.gsakFasade = gsakFasade;
        this.sakService = sakService;
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

        Oppgave oppgave = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.JFR)
            .setTema(tema)
            .setAktørId(aktørID)
            .setPrioritet(PrioritetType.NORM)
            .setJournalpostId(journalpostID)
            .setFristFerdigstillelse(LocalDate.now().plusDays(7))
            .build();

        String oppgaveID = gsakFasade.opprettOppgave(oppgave);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Journalføringsoppgave opprettet med ID {}", oppgaveID);
    }

    private Tema avklarTema(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        if (harGsakSaksnummer(prosessinstans)) {
            Long gsakSaksnummer = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
            return sakService.hentTemaFraSak(gsakSaksnummer);
        }

        return Tema.MED;
    }

    private boolean harGsakSaksnummer(Prosessinstans prosessinstans) {
        return prosessinstans.getBehandling() != null
            && prosessinstans.getBehandling().getFagsak() != null
            && prosessinstans.getBehandling().getFagsak().getGsakSaksnummer() != null;
    }
}
