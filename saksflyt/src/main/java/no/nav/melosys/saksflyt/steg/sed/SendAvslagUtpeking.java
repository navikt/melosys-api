package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class SendAvslagUtpeking implements StegBehandler {

    private final EessiService eessiService;

    public SendAvslagUtpeking(EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.UTPEKING_SEND_AVSLAG;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        long behandlingId = prosessinstans.getBehandling().getId();
        UtpekingAvvis utpekingAvvis = prosessinstans.getData(ProsessDataKey.UTPEKING_AVVIS, UtpekingAvvis.class);
        eessiService.sendAvslagUtpekingSvar(behandlingId, utpekingAvvis);
    }
}
