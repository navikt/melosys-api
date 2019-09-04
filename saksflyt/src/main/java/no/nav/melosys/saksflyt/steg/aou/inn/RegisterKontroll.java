package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.RegisterKontrollFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakRegisterKontroll")
public class RegisterKontroll extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final RegisterKontrollFelles registerKontrollFelles;

    @Autowired
    public RegisterKontroll(RegisterKontrollFelles registerKontrollFelles) {
        this.registerKontrollFelles = registerKontrollFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        registerKontrollFelles.utførKontrollerOgRegistrerFeil(prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE);
    }
}
