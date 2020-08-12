package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AFLOpprettOppgave")
public class OpprettOppgave implements StegBehandler {

    private final OppgaveService oppgaveService;

    public OpprettOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.getBehandling(),
            prosessinstans.hentJournalpostID(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID),
            prosessinstans.hentSaksbehandlerHvisTilordnes()
        );
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
