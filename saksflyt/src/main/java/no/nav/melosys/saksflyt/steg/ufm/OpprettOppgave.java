package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
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

@Component("RegistreringUnntakOpprettOppgave")
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();

        //Midlertidige verdier for oppgave satt til disse er nærmere avklart
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setPrioritet(PrioritetType.NORM);
        oppgaveBuilder.setTema(Tema.MED);
        oppgaveBuilder.setSaksnummer(fagsak.getSaksnummer());
        oppgaveBuilder.setAktørId(prosessinstans.getData(ProsessDataKey.AKTØR_ID));
        oppgaveBuilder.setJournalpostId(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID));
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SED);
        oppgaveBuilder.setBehandlingstema(Behandlingstema.EU_EOS);

        String oppgaveId = gsakFasade.opprettOppgave(oppgaveBuilder.build());
        log.info("Opprettet oppgave {} til manuell behandling for sak {}", oppgaveId, fagsak.getSaksnummer());

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
