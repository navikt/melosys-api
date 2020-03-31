package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettAvgiftsoppgave extends AbstraktStegBehandler {
    private static final long FRIST_AVGIFTSVURDERING_MD = 1;
    static final String AVGIFTSVURDERING_BESKRIVELSE = "Vurderes for innregistrering i Avgiftssystemet";

    private final OppgaveService oppgaveService;

    @Autowired
    public OpprettAvgiftsoppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder()
            .setTema(Tema.TRY).setOppgavetype(Oppgavetyper.VUR)
            .setJournalpostId(behandling.getInitierendeJournalpostId())
            .setBehandlesAvApplikasjon(Fagsystem.INTET)
            .setAktørId(fagsak.hentBruker().getAktørId())
            .setBeskrivelse(AVGIFTSVURDERING_BESKRIVELSE)
            .setFristFerdigstillelse(LocalDate.now().plusMonths(FRIST_AVGIFTSVURDERING_MD))
            .setSaksnummer(fagsak.getSaksnummer());

        oppgaveService.opprettOppgave(oppgaveBuilder.build());

        prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }
}

