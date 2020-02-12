package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.RegisterkontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakRegisterKontroll")
public class RegisterKontroll extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final RegisterkontrollService registerkontrollService;

    @Autowired
    public RegisterKontroll(RegisterkontrollService registerkontrollService) {
        this.registerkontrollService = registerkontrollService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        registerkontrollService.utførKontrollerOgRegistrerFeil(prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL);
    }
}
