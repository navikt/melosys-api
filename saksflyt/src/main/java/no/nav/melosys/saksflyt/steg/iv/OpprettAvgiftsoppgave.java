package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettAvgiftsoppgave extends AbstraktStegBehandler {
    private static final long FRIST_AVGIFTSVURDERING_MD = 1;
    static final String AVGIFTSVURDERING_BESKRIVELSE = "Vurderes for innregistrering i Avgiftssystemet";

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettAvgiftsoppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder()
            .setTema(Tema.TRY).setOppgavetype(Oppgavetyper.VUR)
            .setBehandlesAvApplikasjon(Fagsystem.INTET)
            .setAktørId(fagsak.hentBruker().getAktørId())
            .setBeskrivelse(AVGIFTSVURDERING_BESKRIVELSE)
            .setFristFerdigstillelse(LocalDate.now().plusMonths(FRIST_AVGIFTSVURDERING_MD))
            .setSaksnummer(fagsak.getSaksnummer());

        gsakFasade.opprettOppgave(oppgaveBuilder.build());

        prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }
}

