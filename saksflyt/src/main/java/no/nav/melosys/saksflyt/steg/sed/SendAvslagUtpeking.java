package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendAvslagUtpeking implements StegBehandler {

    private final EessiService eessiService;

    @Autowired
    public SendAvslagUtpeking(@Qualifier("system") EessiService eessiService) {
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
