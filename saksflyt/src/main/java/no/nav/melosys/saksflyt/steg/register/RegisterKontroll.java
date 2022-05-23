package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontroll implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final KontrollresultatService kontrollresultatService;

    public RegisterKontroll(KontrollresultatService kontrollresultatService) {
        this.kontrollresultatService = kontrollresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.REGISTERKONTROLL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        log.info("Utfører registerkontroll for behandling {}", behandlingID);
        kontrollresultatService.utførKontrollerOgRegistrerFeil(behandlingID);
    }
}
