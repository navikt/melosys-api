package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOpprettOppgave")
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);

        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(prosessinstans.getBehandling().getType())
            .setJournalpostId(journalpostId)
            .setAktørId(aktørId)
            .setSaksnummer(saksnummer)
            .build();

        String oppgaveId = gsakFasade.opprettOppgave(oppgave);
        log.info("Opprettet oppgave {} til manuell behandling for sak {}", oppgaveId, saksnummer);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
