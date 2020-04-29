package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakVarsleUtland")
public class VarsleUtland extends AbstraktStegBehandler {

    private final EessiService eessiService;
    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public VarsleUtland(@Qualifier("system") EessiService eessiService, SaksopplysningerService saksopplysningerService) {
        this.eessiService = eessiService;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VARSLE_UTLAND;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Boolean varsleUtland = prosessinstans.getData(ProsessDataKey.VARSLE_UTLAND, Boolean.class);

        if (behandling.erUtpekingAvAnnetLand() && Boolean.TRUE.equals(varsleUtland)) {
            if (saksopplysningerService.hentSedOpplysninger(behandling.getId()).getErElektronisk()) {
                eessiService.sendGodkjenningArbeidFlereLand(behandling.getId());
            } else {
                throw new UnsupportedOperationException("Sending av brev-A012 er ikke implementert");
            }
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }
}
