package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontroll implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final UfmKontrollService ufmKontrollService;

    public RegisterKontroll(UfmKontrollService ufmKontrollService) {
        this.ufmKontrollService = ufmKontrollService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.REGISTERKONTROLL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        log.info("Utfører registerkontroll for behandling {}", behandlingID);
        ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingID);
    }
}
