package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.AbstraktOpprettBehandlingsoppgave;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AFLOpprettOppgave")
public class OpprettOppgave extends AbstraktOpprettBehandlingsoppgave {

    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        super(gsakFasade);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        super.opprettOppgave(prosessinstans);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
