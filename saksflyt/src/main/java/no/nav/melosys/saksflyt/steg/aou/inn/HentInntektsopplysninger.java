package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakHentInntektsopplysninger")
public class HentInntektsopplysninger extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HentInntektsopplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;

    @Autowired
    public HentInntektsopplysninger(HentOpplysningerFelles hentOpplysningerFelles) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_HENT_YTELSER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        long behandlingId = prosessinstans.getBehandling().getId();
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        hentOpplysningerFelles.hentOgLagreInntektsopplysninger(behandlingId, fnr);

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL);
    }
}
