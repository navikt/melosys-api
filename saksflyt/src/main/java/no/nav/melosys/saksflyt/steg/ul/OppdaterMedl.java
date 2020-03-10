package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_OPPDATER_MEDL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_SEND_ORIENTERINGSBREV;

@Component
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final MedlPeriodeService felles;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, MedlPeriodeService felles) {
        this.medlFasade = medlFasade;
        this.felles = felles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return UL_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        String fnr = felles.hentFnr(behandling);
        Lovvalgsperiode lovvalgsperiode = felles.hentLovvalgsperiode(behandling);

        Long medlPeriodeID = medlFasade.opprettPeriodeForeløpig(fnr, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        felles.lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandling.getId());

        prosessinstans.setSteg(UL_SEND_ORIENTERINGSBREV);
    }
}
