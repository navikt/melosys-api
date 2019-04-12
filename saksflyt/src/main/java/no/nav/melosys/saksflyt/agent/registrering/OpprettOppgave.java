package no.nav.melosys.saksflyt.agent.registrering;

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
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOpprettOppgave")
public class OpprettOppgave extends AbstraktStegBehandler {

    private final GsakFasade gsakFasade;

    public OpprettOppgave(GsakFasade gsakFasade) {
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
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();

        //Foreløpig satt verdier for oppgave. Avklares nærmere med MELOSYS-2280
        Oppgave oppgave = new Oppgave();
        oppgave.setPrioritet(PrioritetType.NORM);
        oppgave.setTema(Tema.UFM);
        oppgave.setSaksnummer(fagsak.getSaksnummer());
        oppgave.setAktørId(prosessinstans.getData(ProsessDataKey.AKTØR_ID));
        oppgave.setJournalpostId(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID));
        oppgave.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave.setBehandlingstype(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP);

        gsakFasade.opprettOppgave(oppgave);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
