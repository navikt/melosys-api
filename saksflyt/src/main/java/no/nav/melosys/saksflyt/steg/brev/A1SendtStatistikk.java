package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class A1SendtStatistikk implements StegBehandler {
    private final UtstedtA1Service utstedtA1Service;

    @Autowired
    public A1SendtStatistikk(UtstedtA1Service utstedtA1Service) {
        this.utstedtA1Service = utstedtA1Service;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_STATISTIKK_UTSTEDT_A1;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        utstedtA1Service.sendMeldingOmUtstedtA1(prosessinstans.getBehandling().getId());
    }
}
