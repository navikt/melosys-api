package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.AbstraktOpprettBehandlingsoppgave;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("opprettoppgaveSedNySak")
public class OpprettOppgave extends AbstraktOpprettBehandlingsoppgave {

    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        super(gsakFasade);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        super.opprettOppgave(prosessinstans);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
