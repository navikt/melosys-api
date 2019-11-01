package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakHentInntektsopplysninger")
public class HentInntektsopplysninger extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HentInntektsopplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;
    private final BehandlingService behandlingService;

    @Autowired
    public HentInntektsopplysninger(HentOpplysningerFelles hentOpplysningerFelles, BehandlingService behandlingService) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_HENT_YTELSER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
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
