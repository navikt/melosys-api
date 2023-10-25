package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class SendGodkjenningRegistreringUnntak implements StegBehandler {

    private final EessiService eessiService;

    public SendGodkjenningRegistreringUnntak(EessiService eessiService) {
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
