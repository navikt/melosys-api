package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakRegisterKontroll")
public class RegisterKontroll implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final KontrollresultatService kontrollresultatService;

    @Autowired
    public RegisterKontroll(KontrollresultatService kontrollresultatService) {
        this.kontrollresultatService = kontrollresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        kontrollresultatService.utførKontrollerOgRegistrerFeil(prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL);
    }
}
