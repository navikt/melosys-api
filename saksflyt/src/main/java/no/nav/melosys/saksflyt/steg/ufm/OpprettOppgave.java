package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.AbstraktOpprettBehandlingsoppgave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOpprettOppgave")
public class OpprettOppgave extends AbstraktOpprettBehandlingsoppgave {

    @Autowired
    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        super(gsakFasade);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        super.opprettOppgave(prosessinstans);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
