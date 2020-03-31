package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOpprettOppgave")
public class OpprettOppgave extends AbstraktStegBehandler {

    private final OppgaveService oppgaveService;

    @Autowired
    public OpprettOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        oppgaveService.opprettBehandlingsoppgave(
            prosessinstans.getBehandling(),
            prosessinstans.hentJournalpostID(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID),
            prosessinstans.hentSaksbehandlerHvisTilordnes()
        );

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
