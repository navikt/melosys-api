package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendGodkjenningRegistreringUnntak implements StegBehandler {

    private final EessiService eessiService;

    @Autowired
    public SendGodkjenningRegistreringUnntak(@Qualifier("system") EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_GODKJENNING_REGISTRERING_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();
        Boolean varsleUtland = prosessinstans.getData(ProsessDataKey.VARSLE_UTLAND, Boolean.class);

        if (behandling.erBeslutningLovvalgAnnetLand() && Boolean.TRUE.equals(varsleUtland)) {
            eessiService.sendGodkjenningArbeidFlereLand(behandling.getId(), prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
        }
    }
}
