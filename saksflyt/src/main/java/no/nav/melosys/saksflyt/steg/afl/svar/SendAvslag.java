package no.nav.melosys.saksflyt.steg.afl.svar;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AFL_OPPDATER_MEDL;

@Component("AFLSvarSendAvslag")
public class SendAvslag extends AbstraktStegBehandler {

    private final EessiService eessiService;

    @Autowired
    public SendAvslag(EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SVAR_SEND_AVSLAG;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        long behandlingId = prosessinstans.getBehandling().getId();
        UtpekingAvvis utpekingAvvis = prosessinstans.getData(ProsessDataKey.UTPEKING_AVVIS, UtpekingAvvis.class);

        eessiService.sendAvslagUtpekingSvar(behandlingId, utpekingAvvis);

        prosessinstans.setSteg(AFL_OPPDATER_MEDL);
    }
}
