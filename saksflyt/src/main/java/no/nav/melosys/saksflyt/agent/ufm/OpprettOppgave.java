package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
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

        //Foreløpig satt verdier for oppgave. Ikke avklart hvilke verdier som skal settes per 07.06.19
        Oppgave oppgave = new Oppgave();
        oppgave.setPrioritet(PrioritetType.NORM);
        oppgave.setTema(Tema.UFM);
        oppgave.setSaksnummer(fagsak.getSaksnummer());
        oppgave.setAktørId(prosessinstans.getData(ProsessDataKey.AKTØR_ID));
        oppgave.setJournalpostId(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID));
        oppgave.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave.setBehandlingstype(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);

        gsakFasade.opprettOppgave(oppgave);
        log.info("Opprettet oppgave til manuell behandling for sak {}", fagsak.getSaksnummer());

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
