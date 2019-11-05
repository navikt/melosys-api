package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligerePeriode extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligerePeriode.class);

    private final OppdaterMedlFelles felles;

    @Autowired
    public AvsluttTidligerePeriode(OppdaterMedlFelles felles) {
        this.felles = felles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            felles.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT);
    }
}
