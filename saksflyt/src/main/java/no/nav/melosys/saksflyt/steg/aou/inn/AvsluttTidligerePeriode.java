package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakAvsluttTidligerePeriode")
public class AvsluttTidligerePeriode extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligerePeriode.class);

    private final OppdaterMedlFelles oppdaterMedlFelles;

    @Autowired
    public AvsluttTidligerePeriode(OppdaterMedlFelles oppdaterMedlFelles) {
        this.oppdaterMedlFelles = oppdaterMedlFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            oppdaterMedlFelles.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_SEDDOKUMENT);
    }
}
