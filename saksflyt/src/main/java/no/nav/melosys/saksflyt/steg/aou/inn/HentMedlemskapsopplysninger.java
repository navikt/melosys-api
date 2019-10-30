package no.nav.melosys.saksflyt.steg.aou.inn;

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

@Component("AnmodningUnntakMottakHentMedlemskapsopplysninger")
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;

    @Autowired
    public HentMedlemskapsopplysninger(HentOpplysningerFelles hentOpplysningerFelles) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_HENT_MEDLEMSKAP;
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
            hentOpplysningerFelles.hentOgLagreMedlemskapsopplysninger(behandlingId, fnr);
        } else {
            log.info("Kunne ikke hente inntektopplysninger grunnet feil i periode");
        }

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_HENT_YTELSER);
    }
}
