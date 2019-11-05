package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.LocalDate;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakHentInntektsopplysninger")
public class HentInntektsopplysninger extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HentInntektsopplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;
    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public HentInntektsopplysninger(HentOpplysningerFelles hentOpplysningerFelles, SaksopplysningerService saksopplysningerService) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_HENT_YTELSER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        final long behandlingID = prosessinstans.getBehandling().getId();
        SedDokument sedDokument = saksopplysningerService.hentSedOpplysninger(behandlingID);

        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        if (!PeriodeKontroller.feilIPeriode(fom, tom)) {
            long behandlingId = prosessinstans.getBehandling().getId();
            String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
            hentOpplysningerFelles.hentOgLagreInntektsopplysninger(behandlingId, fnr);
            hentOpplysningerFelles.hentOgLagreUtbetalingsopplysninger(behandlingId, fnr);
        } else {
            log.info("Kunne ikke hente inntektopplysninger grunnet feil i periode");
        }

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL);
    }
}
