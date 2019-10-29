package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("UnntakFraMedlemskapHentInntektOpplysninger")
public class HentInntektOpplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentInntektOpplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;

    @Autowired
    HentInntektOpplysninger(HentOpplysningerFelles hentOpplysningerFelles) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_YTELSER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        LocalDate fom = melosysEessiMelding.getPeriode().getFom();
        LocalDate tom = melosysEessiMelding.getPeriode().getTom();

        if (!PeriodeKontroller.feilIPeriode(fom, tom)) {
            long behandlingId = prosessinstans.getBehandling().getId();
            String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
            hentOpplysningerFelles.hentOgLagreInntektsopplysninger(behandlingId, fnr);
            hentOpplysningerFelles.hentOgLagreUtbetalingsopplysninger(behandlingId, fnr);
        } else {
            log.info("Kunne ikke hente inntektopplysninger grunnet feil i periode");
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }
}
