package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_OPPDATER_MEDL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_SEND_ORIENTERINGSBREV;

@Component("ULOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OppdaterMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return UL_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Lovvalgsperiode lovvalgsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertLovvalgsperiode();
        medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandlingID, false, null);

        prosessinstans.setSteg(UL_SEND_ORIENTERINGSBREV);
    }
}
